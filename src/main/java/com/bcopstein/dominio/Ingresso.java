package com.bcopstein.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Classe abstrata obrigatoria. Cada modalidade concreta decide
 * como calcula o preco e se da direito a entrada antecipada.
 * Quem usa um Ingresso nunca precisa saber qual e o tipo concreto.
 */
public abstract class Ingresso {
    private final String id;
    private final Evento evento;
    private final Participante participante;

    protected Ingresso(String id, Evento evento, Participante participante) {
        if (id == null || id.isBlank()) {
            throw new RegraNegocioException("O ingresso precisa de um identificador.");
        }
        if (evento == null) {
            throw new RegraNegocioException("O ingresso precisa estar vinculado a um evento.");
        }
        this.id = id;
        this.evento = evento;
        this.participante = participante;
    }

    /** Comportamento polimorfico 1: cada modalidade tem sua regra de preco. */
    public abstract BigDecimal calcularPreco();

    /** Comportamento polimorfico 2: so o VIP responde true. */
    public abstract boolean permiteEntradaAntecipada();

    /** Usado para agrupar em relatorios (nao para calcular preco). */
    public abstract Modalidade getModalidade();

    /** Por padrao um ingresso nao consome a cota promocional; Promocional sobrescreve. */
    public boolean consomeCotaPromocional() {
        return false;
    }

    /** Informacao extra para exibir na tela; cada modalidade pode sobrescrever. */
    public String getDetalhe() {
        return "";
    }

    protected BigDecimal precoBase() {
        return evento.getPrecoBase();
    }

    protected static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    public String getId() { return id; }
    public Evento getEvento() { return evento; }
    public Participante getParticipante() { return participante; }

    @Override
    public String toString() {
        return id + " [" + getModalidade() + "] " + participante.nome() + " - R$ " + calcularPreco();
    }
}
