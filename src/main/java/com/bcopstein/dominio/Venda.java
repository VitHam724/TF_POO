package com.bcopstein.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Uma venda: comprador, data/hora e uma lista heterogenea de ingressos
 * (pode misturar Inteira, MeiaEntrada, VIP e Promocional).
 */
public class Venda {
    private final String id;
    private final Evento evento;
    private final String comprador;
    private final LocalDateTime dataHora;
    private final List<Ingresso> ingressos;

    public Venda(String id, Evento evento, String comprador, LocalDateTime dataHora, List<Ingresso> ingressos) {
        if (comprador == null || comprador.isBlank()) {
            throw new RegraNegocioException("O nome do comprador é obrigatório.");
        }
        if (ingressos == null || ingressos.isEmpty()) {
            throw new RegraNegocioException("A venda precisa ter pelo menos um ingresso.");
        }
        if (ingressos.stream().anyMatch(i -> i.getEvento() != evento)) {
            throw new RegraNegocioException("Todos os ingressos devem ser do evento da venda.");
        }
        // CPF nao pode se repetir dentro da mesma venda (Set.add devolve false se ja existia)
        Set<String> cpfs = new HashSet<>();
        for (Ingresso ingresso : ingressos) {
            if (!cpfs.add(ingresso.getParticipante().cpf())) {
                throw new RegraNegocioException("O CPF " + ingresso.getParticipante().cpf()
                        + " aparece mais de uma vez nesta venda.");
            }
        }
        this.id = id;
        this.evento = evento;
        this.comprador = comprador.trim();
        this.dataHora = dataHora;
        this.ingressos = List.copyOf(ingressos);   // lista imutavel
    }

    /** Soma dos precos; cada ingresso calcula o proprio preco (polimorfismo). */
    public BigDecimal getValorTotal() {
        return ingressos.stream()
                .map(Ingresso::calcularPreco)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getQuantidadeIngressos() {
        return ingressos.size();
    }

    public String getId() { return id; }
    public Evento getEvento() { return evento; }
    public String getComprador() { return comprador; }
    public LocalDateTime getDataHora() { return dataHora; }
    public List<Ingresso> getIngressos() { return ingressos; }

    @Override
    public String toString() {
        return id + " - " + comprador + " (" + ingressos.size() + " ingressos, R$ " + getValorTotal() + ")";
    }
}
