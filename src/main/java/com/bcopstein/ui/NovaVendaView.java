package com.bcopstein.ui;

import com.bcopstein.dominio.Evento;
import com.bcopstein.dominio.Modalidade;
import com.bcopstein.dominio.RegraNegocioException;
import com.bcopstein.dominio.Venda;
import com.bcopstein.servico.ConsultasGerenciais;
import com.bcopstein.servico.PedidoIngresso;
import com.bcopstein.servico.SistemaEventos;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Funcionalidades 3 e 4. A tela so monta a lista de pedidos; TODAS as regras
 * sao verificadas em SistemaEventos.registrarVenda (camada de negocio).
 */
@Route(value = "vendas/nova", layout = MainLayout.class)
@PageTitle("Nova venda")
public class NovaVendaView extends VerticalLayout {
    private final SistemaEventos sistema;
    private final ConsultasGerenciais consultas;

    private final ComboBox<Evento> evento = new ComboBox<>("Evento");
    private final Paragraph situacaoEvento = new Paragraph();
    private final TextField comprador = new TextField("Nome do comprador");

    private final Select<Modalidade> modalidade = new Select<>();
    private final TextField nome = new TextField("Nome do participante");
    private final TextField cpf = new TextField("CPF (11 dígitos)");
    private final TextField justificativa = new TextField("Justificativa da meia");

    private final List<PedidoIngresso> pedidos = new ArrayList<>();
    private final Grid<PedidoIngresso> gridPedidos = new Grid<>();
    private final Span total = new Span();

    public NovaVendaView(SistemaEventos sistema, ConsultasGerenciais consultas) {
        this.sistema = sistema;
        this.consultas = consultas;
        setMaxWidth("1100px");

        // A lista mostra todos os eventos de proposito: escolher um evento realizado
        // permite demonstrar a recusa da venda pela regra de negocio.
        evento.setItems(consultas.eventosFiltrados(null, null, null));
        evento.setItemLabelGenerator(e -> e.getId() + " – " + e.getTitulo() + " (" + Formatos.data(e.getData()) + ")");
        evento.setWidth("420px");
        evento.addValueChangeListener(e -> atualizarResumo());
        comprador.setWidth("320px");

        modalidade.setLabel("Modalidade");
        modalidade.setItems(Modalidade.values());
        modalidade.setValue(Modalidade.INTEIRA);
        modalidade.addValueChangeListener(e -> justificativa.setEnabled(e.getValue() == Modalidade.MEIA_ENTRADA));
        justificativa.setEnabled(false);
        cpf.setMaxLength(11);
        cpf.setAllowedCharPattern("[0-9]");

        Button adicionar = new Button("Adicionar ingresso", VaadinIcon.PLUS.create(), e -> adicionarPedido());
        HorizontalLayout linhaIngresso = new HorizontalLayout(modalidade, nome, cpf, justificativa, adicionar);
        linhaIngresso.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        linhaIngresso.setWrap(true);

        gridPedidos.addColumn(p -> p.modalidade().getDescricao()).setHeader("Modalidade");
        gridPedidos.addColumn(PedidoIngresso::nome).setHeader("Participante");
        gridPedidos.addColumn(PedidoIngresso::cpf).setHeader("CPF");
        gridPedidos.addColumn(p -> p.justificativa() == null ? "" : p.justificativa()).setHeader("Justificativa");
        gridPedidos.addColumn(p -> precoEstimado(p.modalidade())).setHeader("Preço");
        gridPedidos.addComponentColumn(p -> new Button(VaadinIcon.TRASH.create(), e -> {
            pedidos.remove(p);
            atualizarPedidos();
        })).setHeader("").setFlexGrow(0);
        gridPedidos.setAllRowsVisible(true);

        Button registrar = new Button("Registrar venda", e -> registrar());
        registrar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new H2("Nova venda"),
                new HorizontalLayout(evento, comprador), situacaoEvento,
                new H3("Ingressos"), linhaIngresso, gridPedidos,
                total, registrar);
        atualizarResumo();
        atualizarPedidos();
    }

    private void adicionarPedido() {
        Modalidade m = modalidade.getValue();
        String just = m == Modalidade.MEIA_ENTRADA ? justificativa.getValue() : null;
        pedidos.add(PedidoIngresso.semId(m, nome.getValue(), cpf.getValue(), just));
        nome.clear();
        cpf.clear();
        justificativa.clear();
        nome.focus();
        atualizarPedidos();
    }

    private void registrar() {
        if (evento.getValue() == null) {
            Formatos.erro("Escolha o evento.");
            return;
        }
        try {
            Venda venda = sistema.registrarVenda(evento.getValue().getId(), comprador.getValue(), List.copyOf(pedidos));
            Formatos.sucesso("Venda " + venda.getId() + " registrada: " + venda.getQuantidadeIngressos()
                    + " ingresso(s), total " + Formatos.moeda(venda.getValorTotal()) + ".");
            pedidos.clear();
            comprador.clear();
            atualizarPedidos();
            atualizarResumo();
        } catch (RegraNegocioException ex) {
            // Nada foi registrado: a lista continua na tela para o usuario corrigir.
            Formatos.erro("Venda não registrada. " + ex.getMessage());
        }
    }

    private String precoEstimado(Modalidade m) {
        if (evento.getValue() == null || m == null) return "–";
        return Formatos.moeda(consultas.precosPorModalidade(evento.getValue()).get(m));
    }

    private void atualizarPedidos() {
        gridPedidos.setItems(pedidos);
        if (evento.getValue() == null) {
            total.setText(pedidos.size() + " ingresso(s) na lista.");
            return;
        }
        Map<Modalidade, BigDecimal> precos = consultas.precosPorModalidade(evento.getValue());
        BigDecimal soma = pedidos.stream()
                .map(p -> precos.get(p.modalidade()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        total.setText(pedidos.size() + " ingresso(s) – total previsto " + Formatos.moeda(soma));
    }

    private void atualizarResumo() {
        Evento e = evento.getValue();
        if (e == null) {
            situacaoEvento.setText("Escolha um evento para ver disponibilidade e preços.");
        } else {
            Map<Modalidade, BigDecimal> precos = consultas.precosPorModalidade(e);
            situacaoEvento.setText((e.jaRealizado(sistema.agora()) ? "Evento já realizado. " : "")
                    + "Disponíveis: " + e.getDisponibilidade()
                    + " | Promocionais disponíveis: " + e.getDisponibilidadePromocional()
                    + " | Inteira " + Formatos.moeda(precos.get(Modalidade.INTEIRA))
                    + ", Meia " + Formatos.moeda(precos.get(Modalidade.MEIA_ENTRADA))
                    + ", VIP " + Formatos.moeda(precos.get(Modalidade.VIP))
                    + ", Promocional " + Formatos.moeda(precos.get(Modalidade.PROMOCIONAL)));
        }
        gridPedidos.getDataProvider().refreshAll();
        atualizarPedidos();
    }
}
