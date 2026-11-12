package com.bcopstein.dados;

import com.bcopstein.dados.LeitorDadosCsv.IngressoCsv;
import com.bcopstein.dados.LeitorDadosCsv.ModalidadeEventoCsv;
import com.bcopstein.dominio.Evento;
import com.bcopstein.dominio.Modalidade;
import com.bcopstein.servico.PedidoIngresso;
import com.bcopstein.servico.SistemaEventos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Converte os registros lidos pelo LeitorDadosCsv (codigo-base do professor)
 * em objetos do dominio. Tudo passa pelo SistemaEventos, entao os dados
 * dos CSV sao validados pelas MESMAS regras usadas na interface.
 */
public final class CarregadorDadosCsv {
    private CarregadorDadosCsv() { }

    public static void carregar(SistemaEventos sistema, Path pasta) throws IOException {
        Map<String, ModalidadeEventoCsv> configuracoes = LeitorDadosCsv
                .lerModalidades(pasta.resolve("modalidades_evento.csv")).stream()
                .collect(Collectors.toMap(ModalidadeEventoCsv::eventoId, Function.identity()));

        for (var e : LeitorDadosCsv.lerEventos(pasta.resolve("eventos.csv"))) {
            ModalidadeEventoCsv config = configuracoes.get(e.id());
            if (config == null) {
                throw new IllegalStateException("Evento " + e.id() + " sem linha em modalidades_evento.csv");
            }
            sistema.cadastrarEvento(new Evento(e.id(), e.titulo(), e.descricao(), e.data(), e.horarioInicio(),
                    e.local(), e.capacidade(), e.precoBase(),
                    config.descontoPromocionalPercentual(), config.limitePromocional()));
        }

        // Agrupa os ingressos pela venda a que pertencem
        Map<String, List<IngressoCsv>> ingressosPorVenda = LeitorDadosCsv
                .lerIngressos(pasta.resolve("ingressos.csv")).stream()
                .collect(Collectors.groupingBy(IngressoCsv::vendaId, LinkedHashMap::new, Collectors.toList()));

        var vendasCsv = LeitorDadosCsv.lerVendas(pasta.resolve("vendas.csv"));
        for (var v : vendasCsv) {
            List<PedidoIngresso> pedidos = ingressosPorVenda.getOrDefault(v.id(), List.of()).stream()
                    .map(i -> new PedidoIngresso(i.id(), Modalidade.deTexto(i.modalidade()),
                            i.participanteNome(), i.participanteCpf(), i.justificativa()))
                    .toList();
            sistema.registrarVenda(v.id(), v.eventoId(), v.comprador(), v.dataHora(), pedidos);
        }

        Set<String> idsVendas = vendasCsv.stream().map(LeitorDadosCsv.VendaCsv::id).collect(Collectors.toSet());
        ingressosPorVenda.keySet().stream()
                .filter(id -> !idsVendas.contains(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new IllegalStateException("ingressos.csv referencia a venda inexistente " + id);
                });
    }
}
