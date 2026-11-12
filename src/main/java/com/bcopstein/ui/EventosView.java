package com.bcopstein.ui;

import com.bcopstein.dominio.Evento;
import com.bcopstein.dominio.Modalidade;
import com.bcopstein.servico.ConsultasGerenciais;
import com.bcopstein.servico.SistemaEventos;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Funcionalidades 1, 5 (disponibilidade), 6, 7 e consulta gerencial "filtrar por periodo/local". */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Eventos")
public class EventosView extends VerticalLayout {
    private static final String TODOS = "Todos", FUTUROS = "Futuros", REALIZADOS = "Realizados";

    private final SistemaEventos sistema;
    private final ConsultasGerenciais consultas;
    private final Grid<Evento> grid = new Grid<>();
    private final Select<String> situacao = new Select<>();
    private final DatePicker de = new DatePicker("De");
    private final DatePicker ate = new DatePicker("Até");
    private final ComboBox<String> local = new ComboBox<>("Local");

    public EventosView(SistemaEventos sistema, ConsultasGerenciais consultas) {
        this.sistema = sistema;
        this.consultas = consultas;
        setSizeFull();

        situacao.setLabel("Situação");
        situacao.setItems(TODOS, FUTUROS, REALIZADOS);
        situacao.setValue(TODOS);
        de.setLocale(Formatos.BR);
        ate.setLocale(Formatos.BR);
        local.setItems(consultas.locais());
        local.setClearButtonVisible(true);
        Button limpar = new Button("Limpar filtros", e -> {
            situacao.setValue(TODOS);
            de.clear();
            ate.clear();
            local.clear();
        });
        situacao.addValueChangeListener(e -> atualizar());
        de.addValueChangeListener(e -> atualizar());
        ate.addValueChangeListener(e -> atualizar());
        local.addValueChangeListener(e -> atualizar());

        HorizontalLayout filtros = new HorizontalLayout(situacao, de, ate, local, limpar);
        filtros.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        filtros.setWrap(true);

        grid.addColumn(Evento::getId).setHeader("Código").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Evento::getTitulo).setHeader("Título").setAutoWidth(true);
        grid.addColumn(e -> Formatos.data(e.getData())).setHeader("Data").setAutoWidth(true);
        grid.addColumn(Evento::getHorarioInicio).setHeader("Início").setAutoWidth(true);
        grid.addColumn(Evento::getLocal).setHeader("Local").setAutoWidth(true);
        grid.addColumn(e -> e.getQuantidadeVendida() + " / " + e.getCapacidade()).setHeader("Vendidos").setAutoWidth(true);
        grid.addColumn(Evento::getDisponibilidade).setHeader("Disponíveis").setAutoWidth(true);
        grid.addColumn(e -> Formatos.moeda(e.getPrecoBase())).setHeader("Preço-base").setAutoWidth(true);
        grid.addColumn(e -> e.jaRealizado(sistema.agora()) ? "Realizado" : "Futuro").setHeader("Situação").setAutoWidth(true);
        grid.addItemClickListener(e -> abrirDetalhes(e.getItem()));
        grid.setSizeFull();

        add(new H2("Eventos"), new Paragraph("Clique em um evento para ver os detalhes."), filtros, grid);
        atualizar();
    }

    private void atualizar() {
        List<Evento> filtrados = consultas.eventosFiltrados(de.getValue(), ate.getValue(), local.getValue());
        List<Evento> base = switch (situacao.getValue()) {
            case FUTUROS -> consultas.eventosFuturos();
            case REALIZADOS -> consultas.eventosRealizados();
            default -> consultas.eventosFiltrados(null, null, null);
        };
        grid.setItems(base.stream().filter(filtrados::contains).toList());
    }

    private void abrirDetalhes(Evento evento) {
        Dialog dialogo = new Dialog();
        dialogo.setHeaderTitle(evento.getId() + " – " + evento.getTitulo());
        dialogo.setWidth("min(720px, 95vw)");

        FormLayout info = new FormLayout();
        info.addFormItem(new Span(evento.getDescricao()), "Descrição");
        info.addFormItem(new Span(Formatos.dataHora(evento.getInicio())), "Início");
        info.addFormItem(new Span(evento.getLocal()), "Local");
        info.addFormItem(new Span(evento.jaRealizado(sistema.agora()) ? "Realizado (não aceita vendas)" : "Futuro"), "Situação");
        info.addFormItem(new Span(String.valueOf(evento.getCapacidade())), "Capacidade");
        info.addFormItem(new Span(evento.getQuantidadeVendida() + " (" + Formatos.percentual(evento.getPercentualOcupacao()) + ")"), "Vendidos / ocupação");
        info.addFormItem(new Span(String.valueOf(evento.getDisponibilidade())), "Disponibilidade geral");
        info.addFormItem(new Span(evento.getDisponibilidadePromocional() + " de " + evento.getLimitePromocional()
                + " (" + evento.getPercentualDescontoPromocional() + "% de desconto)"), "Promocionais disponíveis");
        info.addFormItem(new Span(Formatos.moeda(evento.getArrecadacao())), "Arrecadação");
        List<Modalidade> maisVendidas = consultas.modalidadesMaisVendidas(evento);
        info.addFormItem(new Span(maisVendidas.isEmpty() ? "Sem vendas"
                : maisVendidas.stream().map(Modalidade::getDescricao).collect(Collectors.joining(", "))), "Modalidade mais vendida");
        info.addFormItem(new Span(evento.getQuantidadeComEntradaAntecipada() + " participante(s), a partir das "
                + evento.getInicioEntradaAntecipada().toLocalTime()), "Entrada antecipada (VIP)");

        Map<Modalidade, java.math.BigDecimal> precos = consultas.precosPorModalidade(evento);
        Map<Modalidade, Long> quantidades = consultas.quantidadePorModalidade(evento);
        Map<Modalidade, java.math.BigDecimal> arrecadacao = consultas.arrecadacaoPorModalidade(evento);
        Grid<Modalidade> porModalidade = new Grid<>();
        porModalidade.addColumn(Modalidade::getDescricao).setHeader("Modalidade");
        porModalidade.addColumn(m -> Formatos.moeda(precos.get(m))).setHeader("Preço unitário");
        porModalidade.addColumn(quantidades::get).setHeader("Vendidos");
        porModalidade.addColumn(m -> Formatos.moeda(arrecadacao.get(m))).setHeader("Arrecadação");
        porModalidade.setItems(Modalidade.values());
        porModalidade.setAllRowsVisible(true);

        dialogo.add(info, new H3("Por modalidade"), porModalidade);
        dialogo.getFooter().add(new Button("Fechar", e -> dialogo.close()));
        dialogo.open();
    }
}
