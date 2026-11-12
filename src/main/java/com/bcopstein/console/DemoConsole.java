package com.bcopstein.console;

import com.bcopstein.dados.CarregadorDadosCsv;
import com.bcopstein.dominio.Evento;
import com.bcopstein.dominio.Modalidade;
import com.bcopstein.dominio.RegraNegocioException;
import com.bcopstein.servico.ConsultasGerenciais;
import com.bcopstein.servico.PedidoIngresso;
import com.bcopstein.servico.SistemaEventos;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

/**
 * Demonstracao em modo texto, sem Vaadin. Util para testar a logica de negocio
 * rapidamente e para estudar as consultas.
 * Executar: mvn compile exec:java -Dexec.mainClass=com.bcopstein.console.DemoConsole
 */
public class DemoConsole {

    public static void main(String[] args) throws Exception {
        SistemaEventos sistema = new SistemaEventos(Clock.systemDefaultZone());
        CarregadorDadosCsv.carregar(sistema, Path.of("dados"));
        ConsultasGerenciais consultas = new ConsultasGerenciais(sistema);

        System.out.println("Agora: " + sistema.agora());
        System.out.println("\n== Eventos futuros (por data) ==");
        consultas.eventosFuturos().forEach(e -> System.out.println("  " + e + " em " + e.getInicio()));
        System.out.println("\n== Eventos realizados ==");
        consultas.eventosRealizados().forEach(e -> System.out.println("  " + e));

        System.out.println("\n== Por evento: vendidos | arrecadação | ocupação | entrada antecipada | mais vendida ==");
        var vendidos = consultas.ingressosVendidosPorEvento();
        var arrecadacao = consultas.arrecadacaoPorEvento();
        var ocupacao = consultas.percentualOcupacaoPorEvento();
        var antecipada = consultas.entradaAntecipadaPorEvento();
        for (Evento e : vendidos.keySet()) {
            System.out.printf("  %-28s %4d | R$ %9s | %5.1f%% | %3d | %s%n", e, vendidos.get(e),
                    arrecadacao.get(e), ocupacao.get(e), antecipada.get(e), consultas.modalidadesMaisVendidas(e));
        }

        System.out.println("\n== Ranking por ingressos vendidos ==");
        List<Evento> ranking = consultas.rankingPorIngressosVendidos();
        for (int i = 0; i < ranking.size(); i++) {
            System.out.println("  " + (i + 1) + "º " + ranking.get(i) + " - " + ranking.get(i).getQuantidadeVendida());
        }

        System.out.println("\n== Arrecadação por modalidade (geral) ==");
        consultas.arrecadacaoPorModalidade().forEach((m, v) -> System.out.println("  " + m + ": R$ " + v));

        System.out.println("\n== Preços no E001 (polimorfismo) ==");
        Evento e001 = sistema.buscarEvento("E001").orElseThrow();
        consultas.precosPorModalidade(e001).forEach((m, v) -> System.out.println("  " + m + ": R$ " + v));

        System.out.println("\n== Tentativas de venda ==");
        tentar(sistema, "Venda válida mista no E001 (inteira + meia + VIP)", "E001", List.of(
                PedidoIngresso.semId(Modalidade.INTEIRA, "Teste A", "11111111111", null),
                PedidoIngresso.semId(Modalidade.MEIA_ENTRADA, "Teste B", "22222222222", "Estudante"),
                PedidoIngresso.semId(Modalidade.VIP, "Teste C", "33333333333", null)));
        tentar(sistema, "Promocional no E001 (cota esgotada)", "E001", List.of(
                PedidoIngresso.semId(Modalidade.INTEIRA, "Teste D", "44444444444", null),
                PedidoIngresso.semId(Modalidade.PROMOCIONAL, "Teste E", "55555555555", null)));
        tentar(sistema, "Evento passado (E004)", "E004", List.of(
                PedidoIngresso.semId(Modalidade.INTEIRA, "Teste F", "66666666666", null)));
        tentar(sistema, "CPF repetido na mesma venda", "E002", List.of(
                PedidoIngresso.semId(Modalidade.INTEIRA, "Teste G", "77777777777", null),
                PedidoIngresso.semId(Modalidade.VIP, "Teste H", "77777777777", null)));
        tentar(sistema, "Meia sem justificativa", "E002", List.of(
                PedidoIngresso.semId(Modalidade.MEIA_ENTRADA, "Teste I", "88888888888", "")));
        System.out.println("\nE001 depois das tentativas: vendidos=" + e001.getQuantidadeVendida()
                + ", disponíveis=" + e001.getDisponibilidade()
                + ", promocionais disponíveis=" + e001.getDisponibilidadePromocional());
    }

    private static void tentar(SistemaEventos sistema, String titulo, String eventoId, List<PedidoIngresso> pedidos) {
        try {
            var venda = sistema.registrarVenda(eventoId, "Comprador Teste", pedidos);
            System.out.println("  OK   " + titulo + " -> " + venda);
            venda.getIngressos().forEach(i -> System.out.println("         " + i + "  " + i.getDetalhe()));
        } catch (RegraNegocioException e) {
            System.out.println("  ERRO " + titulo + " -> " + e.getMessage());
        }
    }
}
