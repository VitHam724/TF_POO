package com.bcopstein.servico;

import com.bcopstein.dominio.Evento;
import com.bcopstein.dominio.Ingresso;
import com.bcopstein.dominio.Modalidade;
import com.bcopstein.dominio.Participante;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Consultas gerenciais obrigatorias (item 8 do enunciado), todas com Stream API.
 * Os mapas por evento sempre incluem TODOS os eventos (com zero quando nao ha vendas).
 */
public class ConsultasGerenciais {
    private final SistemaEventos sistema;

    public ConsultasGerenciais(SistemaEventos sistema) {
        this.sistema = sistema;
    }

    /** Todos os ingressos vendidos, de todos os eventos (colecao heterogenea). */
    private Stream<Ingresso> todosIngressos() {
        return sistema.listarVendas().stream()
                .flatMap(v -> v.getIngressos().stream());
    }

    private Stream<Evento> eventosPorData() {
        return sistema.listarEventos().stream()
                .sorted(Comparator.comparing(Evento::getInicio));
    }

    // 1. Eventos futuros ordenados por data
    public List<Evento> eventosFuturos() {
        return eventosPorData()
                .filter(e -> !e.jaRealizado(sistema.agora()))
                .toList();
    }

    // (funcionalidade 6) eventos ja realizados, do mais recente para o mais antigo
    public List<Evento> eventosRealizados() {
        return sistema.listarEventos().stream()
                .filter(e -> e.jaRealizado(sistema.agora()))
                .sorted(Comparator.comparing(Evento::getInicio).reversed())
                .toList();
    }

    // 2. Eventos filtrados por periodo e/ou local (parametros nulos sao ignorados)
    public List<Evento> eventosFiltrados(LocalDate inicio, LocalDate fim, String local) {
        return eventosPorData()
                .filter(e -> inicio == null || !e.getData().isBefore(inicio))
                .filter(e -> fim == null || !e.getData().isAfter(fim))
                .filter(e -> local == null || local.isBlank() || e.getLocal().equalsIgnoreCase(local.trim()))
                .toList();
    }

    public List<String> locais() {
        return sistema.listarEventos().stream()
                .map(Evento::getLocal)
                .distinct()
                .sorted()
                .toList();
    }

    // 3. Quantidade de ingressos vendidos por evento (groupingBy + summingInt)
    public Map<Evento, Integer> ingressosVendidosPorEvento() {
        Map<Evento, Integer> vendidos = todosIngressos()
                .collect(Collectors.groupingBy(Ingresso::getEvento, Collectors.summingInt(i -> 1)));
        return paraTodosOsEventos(vendidos, 0);
    }

    // 4. Arrecadacao total por evento (groupingBy + reducing com BigDecimal)
    public Map<Evento, BigDecimal> arrecadacaoPorEvento() {
        Map<Evento, BigDecimal> arrecadacao = todosIngressos()
                .collect(Collectors.groupingBy(Ingresso::getEvento,
                        Collectors.reducing(BigDecimal.ZERO, Ingresso::calcularPreco, BigDecimal::add)));
        return paraTodosOsEventos(arrecadacao, BigDecimal.ZERO);
    }

    // 5. Arrecadacao agrupada por modalidade (geral)
    public Map<Modalidade, BigDecimal> arrecadacaoPorModalidade() {
        return arrecadacaoPorModalidade(todosIngressos());
    }

    // 5b. Arrecadacao por modalidade de um evento
    public Map<Modalidade, BigDecimal> arrecadacaoPorModalidade(Evento evento) {
        return arrecadacaoPorModalidade(evento.ingressosVendidos());
    }

    private Map<Modalidade, BigDecimal> arrecadacaoPorModalidade(Stream<Ingresso> ingressos) {
        Map<Modalidade, BigDecimal> resultado = ingressos.collect(Collectors.groupingBy(
                Ingresso::getModalidade,
                () -> new EnumMap<>(Modalidade.class),
                Collectors.reducing(BigDecimal.ZERO, Ingresso::calcularPreco, BigDecimal::add)));
        Arrays.stream(Modalidade.values()).forEach(m -> resultado.putIfAbsent(m, BigDecimal.ZERO));
        return resultado;
    }

    public Map<Modalidade, Long> quantidadePorModalidade() {
        return quantidadePorModalidade(todosIngressos());
    }

    public Map<Modalidade, Long> quantidadePorModalidade(Evento evento) {
        return quantidadePorModalidade(evento.ingressosVendidos());
    }

    private Map<Modalidade, Long> quantidadePorModalidade(Stream<Ingresso> ingressos) {
        Map<Modalidade, Long> resultado = ingressos.collect(Collectors.groupingBy(
                Ingresso::getModalidade, () -> new EnumMap<>(Modalidade.class), Collectors.counting()));
        Arrays.stream(Modalidade.values()).forEach(m -> resultado.putIfAbsent(m, 0L));
        return resultado;
    }

    // 6. Ranking de eventos por quantidade de ingressos vendidos (desempate pelo titulo)
    public List<Evento> rankingPorIngressosVendidos() {
        return sistema.listarEventos().stream()
                .sorted(Comparator.comparingInt(Evento::getQuantidadeVendida).reversed()
                        .thenComparing(Evento::getTitulo))
                .toList();
    }

    // 7. Percentual de ocupacao de cada evento
    public Map<Evento, Double> percentualOcupacaoPorEvento() {
        return eventosPorData()
                .collect(Collectors.toMap(Function.identity(), Evento::getPercentualOcupacao,
                        (a, b) -> a, LinkedHashMap::new));
    }

    // 8. Modalidade(s) mais vendida(s) em um evento - lista para tratar empates
    public List<Modalidade> modalidadesMaisVendidas(Evento evento) {
        Map<Modalidade, Long> quantidades = quantidadePorModalidade(evento);
        long maximo = quantidades.values().stream().mapToLong(Long::longValue).max().orElse(0);
        if (maximo == 0) {
            return List.of();
        }
        return quantidades.entrySet().stream()
                .filter(e -> e.getValue() == maximo)
                .map(Map.Entry::getKey)
                .toList();
    }

    // 9. Participantes com direito a entrada antecipada, por evento
    //    (filtra pelo metodo polimorfico permiteEntradaAntecipada, sem instanceof)
    public Map<Evento, Long> entradaAntecipadaPorEvento() {
        Map<Evento, Long> contagem = todosIngressos()
                .filter(Ingresso::permiteEntradaAntecipada)
                .collect(Collectors.groupingBy(Ingresso::getEvento, Collectors.counting()));
        return paraTodosOsEventos(contagem, 0L);
    }

    /**
     * Tabela de precos de um evento: cria um ingresso "de simulacao" de cada
     * modalidade e pede o preco a cada um. E polimorfismo puro: a mesma
     * chamada calcularPreco() da resultados diferentes conforme o tipo.
     */
    public Map<Modalidade, BigDecimal> precosPorModalidade(Evento evento) {
        Participante simulacao = new Participante("Simulação", "00000000000");
        Map<Modalidade, BigDecimal> precos = new EnumMap<>(Modalidade.class);
        for (Modalidade m : Modalidade.values()) {
            Ingresso exemplo = m.criarIngresso("SIMULACAO", evento, simulacao, "simulação");
            precos.put(m, exemplo.calcularPreco());
        }
        return precos;
    }

    /** Garante uma entrada para cada evento (ordenados por data), usando 'zero' se faltar. */
    private <V> Map<Evento, V> paraTodosOsEventos(Map<Evento, V> parcial, V zero) {
        return eventosPorData()
                .collect(Collectors.toMap(Function.identity(),
                        e -> parcial.getOrDefault(e, zero),
                        (a, b) -> a, LinkedHashMap::new));
    }
}
