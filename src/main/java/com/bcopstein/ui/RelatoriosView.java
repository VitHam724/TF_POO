package com.bcopstein.ui;

import com.bcopstein.dominio.Evento;
import com.bcopstein.dominio.Modalidade;
import com.bcopstein.servico.ConsultasGerenciais;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Consultas gerenciais obrigatorias (item 8). Cada coluna vem de um metodo de ConsultasGerenciais. */
@Route(value = "relatorios", layout = MainLayout.class)
@PageTitle("Relatórios")
public class RelatoriosView extends VerticalLayout {
    private final ConsultasGerenciais consultas;

    public RelatoriosView(ConsultasGerenciais consultas) {
        this.consultas = consultas;
        setSizeFull();

        TabSheet abas = new TabSheet();
        abas.add("Por evento", porEvento());
        abas.add("Ranking", ranking());
        abas.add("Por modalidade", porModalidade());
        abas.add("Eventos futuros", futuros());
        abas.setSizeFull();

        add(new H2("Relatórios gerenciais"), abas);
    }

    private VerticalLayout porEvento() {
        Map<Evento, Integer> vendidos = consultas.ingressosVendidosPorEvento();
        Map<Evento, BigDecimal> arrecadacao = consultas.arrecadacaoPorEvento();
        Map<Evento, Double> ocupacao = consultas.percentualOcupacaoPorEvento();
        Map<Evento, Long> antecipada = consultas.entradaAntecipadaPorEvento();

        Grid<Evento> grid = new Grid<>();
        grid.addColumn(Evento::toString).setHeader("Evento").setAutoWidth(true);
        grid.addColumn(vendidos::get).setHeader("Ingressos vendidos").setAutoWidth(true);
        grid.addColumn(e -> Formatos.moeda(arrecadacao.get(e))).setHeader("Arrecadação").setAutoWidth(true);
        grid.addColumn(e -> Formatos.percentual(ocupacao.get(e))).setHeader("Ocupação").setAutoWidth(true);
        grid.addColumn(e -> textoModalidades(consultas.modalidadesMaisVendidas(e))).setHeader("Modalidade mais vendida").setAutoWidth(true);
        grid.addColumn(antecipada::get).setHeader("Entrada antecipada").setAutoWidth(true);
        grid.setItems(vendidos.keySet());
        grid.setAllRowsVisible(true);
        return new VerticalLayout(grid);
    }

    private VerticalLayout ranking() {
        List<Evento> ranking = consultas.rankingPorIngressosVendidos();
        Grid<Evento> grid = new Grid<>();
        grid.addColumn(e -> (ranking.indexOf(e) + 1) + "º").setHeader("Posição").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Evento::toString).setHeader("Evento").setAutoWidth(true);
        grid.addColumn(Evento::getQuantidadeVendida).setHeader("Ingressos vendidos").setAutoWidth(true);
        grid.setItems(ranking);
        grid.setAllRowsVisible(true);
        return new VerticalLayout(grid);
    }

    private VerticalLayout porModalidade() {
        ComboBox<Evento> filtro = new ComboBox<>("Evento");
        filtro.setPlaceholder("Todos os eventos");
        filtro.setItems(consultas.eventosFiltrados(null, null, null));
        filtro.setClearButtonVisible(true);
        filtro.setWidth("360px");

        Grid<Modalidade> grid = new Grid<>();
        Runnable atualizar = () -> {
            Evento e = filtro.getValue();
            Map<Modalidade, Long> qtd = e == null ? consultas.quantidadePorModalidade() : consultas.quantidadePorModalidade(e);
            Map<Modalidade, BigDecimal> valor = e == null ? consultas.arrecadacaoPorModalidade() : consultas.arrecadacaoPorModalidade(e);
            grid.removeAllColumns();
            grid.addColumn(Modalidade::getDescricao).setHeader("Modalidade");
            grid.addColumn(qtd::get).setHeader("Ingressos");
            grid.addColumn(m -> Formatos.moeda(valor.get(m))).setHeader("Arrecadação");
            grid.setItems(Modalidade.values());
        };
        filtro.addValueChangeListener(ev -> atualizar.run());
        atualizar.run();
        grid.setAllRowsVisible(true);
        return new VerticalLayout(filtro, grid);
    }

    private VerticalLayout futuros() {
        Grid<Evento> grid = new Grid<>();
        grid.addColumn(e -> Formatos.dataHora(e.getInicio())).setHeader("Início").setAutoWidth(true);
        grid.addColumn(Evento::toString).setHeader("Evento").setAutoWidth(true);
        grid.addColumn(Evento::getLocal).setHeader("Local").setAutoWidth(true);
        grid.addColumn(Evento::getDisponibilidade).setHeader("Disponíveis").setAutoWidth(true);
        grid.setItems(consultas.eventosFuturos());
        grid.setAllRowsVisible(true);
        return new VerticalLayout(new Paragraph("Ordenados por data. O filtro por período e local fica na tela Eventos."), grid);
    }

    private static String textoModalidades(List<Modalidade> modalidades) {
        return modalidades.isEmpty() ? "Sem vendas"
                : modalidades.stream().map(Modalidade::getDescricao).collect(Collectors.joining(", "));
    }
}
