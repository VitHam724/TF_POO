package com.bcopstein.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Evento com suas configuracoes (inclusive as da modalidade promocional)
 * e as vendas ja registradas. Nao tem setters: depois de cadastrado,
 * nao pode ser editado (regra 3.1).
 */
public class Evento {
    private final String id;
    private final String titulo;
    private final String descricao;
    private final LocalDate data;
    private final LocalTime horarioInicio;
    private final String local;
    private final int capacidade;
    private final BigDecimal precoBase;
    private final int percentualDescontoPromocional;
    private final int limitePromocional;
    private final List<Venda> vendas = new ArrayList<>();

    public Evento(String id, String titulo, String descricao, LocalDate data, LocalTime horarioInicio,
                  String local, int capacidade, BigDecimal precoBase,
                  int percentualDescontoPromocional, int limitePromocional) {
        if (id == null || id.isBlank()) throw new RegraNegocioException("O identificador do evento é obrigatório.");
        if (titulo == null || titulo.isBlank()) throw new RegraNegocioException("O título é obrigatório.");
        if (local == null || local.isBlank()) throw new RegraNegocioException("O local é obrigatório.");
        if (data == null || horarioInicio == null) throw new RegraNegocioException("Data e horário são obrigatórios.");
        if (capacidade <= 0) throw new RegraNegocioException("A capacidade deve ser maior que zero.");
        if (precoBase == null || precoBase.signum() < 0) throw new RegraNegocioException("O preço-base não pode ser negativo.");
        if (percentualDescontoPromocional < 0 || percentualDescontoPromocional > 100)
            throw new RegraNegocioException("O desconto promocional deve estar entre 0 e 100%.");
        if (limitePromocional < 0 || limitePromocional > capacidade)
            throw new RegraNegocioException("O limite promocional deve estar entre 0 e a capacidade do evento.");

        this.id = id.trim();
        this.titulo = titulo.trim();
        this.descricao = descricao == null ? "" : descricao.trim();
        this.data = data;
        this.horarioInicio = horarioInicio;
        this.local = local.trim();
        this.capacidade = capacidade;
        this.precoBase = precoBase;
        this.percentualDescontoPromocional = percentualDescontoPromocional;
        this.limitePromocional = limitePromocional;
    }

    // ---------- Registro de venda (validacao "tudo ou nada") ----------

    /**
     * Valida a venda inteira e so entao a adiciona. Se qualquer regra falhar,
     * lanca excecao ANTES do add: nenhum ingresso fica registrado.
     */
    public void registrarVenda(Venda venda) {
        if (venda.getEvento() != this) {
            throw new RegraNegocioException("A venda pertence a outro evento.");
        }
        if (!venda.getDataHora().isBefore(getInicio())) {
            throw new RegraNegocioException("O evento \"" + titulo + "\" já foi realizado (ou já começou) "
                    + "e não aceita novas vendas.");
        }
        int quantidade = venda.getQuantidadeIngressos();
        if (quantidade > getDisponibilidade()) {
            throw new RegraNegocioException("Capacidade insuficiente: a venda tem " + quantidade
                    + " ingresso(s), mas restam apenas " + getDisponibilidade() + " lugar(es).");
        }
        long promocionais = venda.getIngressos().stream()
                .filter(Ingresso::consomeCotaPromocional)
                .count();
        if (promocionais > getCotaPromocionalRestante()) {
            throw new RegraNegocioException("Cota promocional insuficiente: a venda tem " + promocionais
                    + " ingresso(s) promocional(is), mas restam apenas " + getCotaPromocionalRestante() + ".");
        }
        vendas.add(venda);
    }

    // ---------- Consultas sobre o proprio evento ----------

    public Stream<Ingresso> ingressosVendidos() {
        return vendas.stream().flatMap(v -> v.getIngressos().stream());
    }

    public int getQuantidadeVendida() {
        return vendas.stream().mapToInt(Venda::getQuantidadeIngressos).sum();
    }

    public int getQuantidadePromocionalVendida() {
        return (int) ingressosVendidos().filter(Ingresso::consomeCotaPromocional).count();
    }

    public int getDisponibilidade() {
        return capacidade - getQuantidadeVendida();
    }

    public int getCotaPromocionalRestante() {
        return limitePromocional - getQuantidadePromocionalVendida();
    }

    /** Promocionais que ainda podem ser vendidos: limitado pela cota E pelos lugares livres. */
    public int getDisponibilidadePromocional() {
        return Math.min(getCotaPromocionalRestante(), getDisponibilidade());
    }

    public BigDecimal getArrecadacao() {
        return ingressosVendidos()
                .map(Ingresso::calcularPreco)          // polimorfismo: cada tipo calcula o seu
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public double getPercentualOcupacao() {
        return getQuantidadeVendida() * 100.0 / capacidade;
    }

    public long getQuantidadeComEntradaAntecipada() {
        return ingressosVendidos().filter(Ingresso::permiteEntradaAntecipada).count();
    }

    public LocalDateTime getInicio() {
        return LocalDateTime.of(data, horarioInicio);
    }

    public LocalDateTime getInicioEntradaAntecipada() {
        return getInicio().minusMinutes(VIP.MINUTOS_ANTECIPACAO);
    }

    /** Evento ja realizado em relacao ao instante informado. */
    public boolean jaRealizado(LocalDateTime agora) {
        return !getInicio().isAfter(agora);
    }

    // ---------- Getters ----------

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public LocalDate getData() { return data; }
    public LocalTime getHorarioInicio() { return horarioInicio; }
    public String getLocal() { return local; }
    public int getCapacidade() { return capacidade; }
    public BigDecimal getPrecoBase() { return precoBase; }
    public int getPercentualDescontoPromocional() { return percentualDescontoPromocional; }
    public int getLimitePromocional() { return limitePromocional; }
    public List<Venda> getVendas() { return Collections.unmodifiableList(vendas); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Evento outro && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id + " - " + titulo;
    }
}
