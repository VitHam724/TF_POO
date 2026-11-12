package com.bcopstein.ui;

import com.bcopstein.dominio.Evento;
import com.bcopstein.dominio.Ingresso;
import com.bcopstein.dominio.Venda;
import com.bcopstein.servico.ConsultasGerenciais;
import com.bcopstein.servico.SistemaEventos;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

/** Funcionalidade 5: consultar vendas e seus ingressos individuais. */
@Route(value = "vendas", layout = MainLayout.class)
@PageTitle("Vendas")
public class VendasView extends VerticalLayout {
    private final SistemaEventos sistema;
    private final ComboBox<Evento> filtroEvento = new ComboBox<>("Filtrar por evento");
    private final Grid<Venda> gridVendas = new Grid<>();
    private final Grid<Ingresso> gridIngressos = new Grid<>();
    private final H3 tituloIngressos = new H3("Selecione uma venda para ver os ingressos");

    public VendasView(SistemaEventos sistema, ConsultasGerenciais consultas) {
        this.sistema = sistema;
        setSizeFull();

        filtroEvento.setItems(consultas.eventosFiltrados(null, null, null));
        filtroEvento.setItemLabelGenerator(e -> e.getId() + " – " + e.getTitulo());
        filtroEvento.setClearButtonVisible(true);
        filtroEvento.setWidth("360px");
        filtroEvento.addValueChangeListener(e -> atualizar());

        gridVendas.addColumn(Venda::getId).setHeader("Venda").setAutoWidth(true);
        gridVendas.addColumn(v -> v.getEvento().getTitulo()).setHeader("Evento").setAutoWidth(true);
        gridVendas.addColumn(Venda::getComprador).setHeader("Comprador").setAutoWidth(true);
        gridVendas.addColumn(v -> Formatos.dataHora(v.getDataHora())).setHeader("Data/hora").setAutoWidth(true);
        gridVendas.addColumn(Venda::getQuantidadeIngressos).setHeader("Ingressos").setAutoWidth(true);
        gridVendas.addColumn(v -> Formatos.moeda(v.getValorTotal())).setHeader("Total").setAutoWidth(true);
        gridVendas.setHeight("40%");
        gridVendas.asSingleSelect().addValueChangeListener(e -> mostrarIngressos(e.getValue()));

        gridIngressos.addColumn(Ingresso::getId).setHeader("Ingresso").setAutoWidth(true);
        gridIngressos.addColumn(i -> i.getModalidade().getDescricao()).setHeader("Modalidade").setAutoWidth(true);
        gridIngressos.addColumn(i -> i.getParticipante().nome()).setHeader("Participante").setAutoWidth(true);
        gridIngressos.addColumn(i -> i.getParticipante().cpf()).setHeader("CPF").setAutoWidth(true);
        gridIngressos.addColumn(i -> Formatos.moeda(i.calcularPreco())).setHeader("Preço").setAutoWidth(true);
        gridIngressos.addColumn(i -> i.permiteEntradaAntecipada() ? "Sim" : "Não").setHeader("Entrada antecipada").setAutoWidth(true);
        gridIngressos.addColumn(Ingresso::getDetalhe).setHeader("Detalhe").setAutoWidth(true);
        gridIngressos.setHeight("40%");

        add(new H2("Vendas"), filtroEvento, gridVendas, tituloIngressos, gridIngressos);
        atualizar();
    }

    private void atualizar() {
        List<Venda> vendas = filtroEvento.getValue() == null
                ? sistema.listarVendas()
                : sistema.listarVendasDoEvento(filtroEvento.getValue());
        gridVendas.setItems(vendas);
        mostrarIngressos(null);
    }

    private void mostrarIngressos(Venda venda) {
        if (venda == null) {
            tituloIngressos.setText("Selecione uma venda para ver os ingressos");
            gridIngressos.setItems(List.of());
        } else {
            tituloIngressos.setText("Ingressos da venda " + venda.getId() + " (" + venda.getComprador() + ")");
            gridIngressos.setItems(venda.getIngressos());
        }
    }
}
