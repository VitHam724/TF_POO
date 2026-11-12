package com.bcopstein.dominio;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/** 150% do preco-base; unica modalidade com entrada antecipada (60 min). */
public class VIP extends Ingresso {
    public static final int MINUTOS_ANTECIPACAO = 60;
    private static final BigDecimal FATOR = new BigDecimal("1.50");

    public VIP(String id, Evento evento, Participante participante) {
        super(id, evento, participante);
    }

    @Override
    public BigDecimal calcularPreco() {
        return arredondar(precoBase().multiply(FATOR));
    }

    @Override
    public boolean permiteEntradaAntecipada() {
        return true;
    }

    @Override
    public Modalidade getModalidade() {
        return Modalidade.VIP;
    }

    @Override
    public String getDetalhe() {
        return "Entrada a partir das "
                + getEvento().getInicioEntradaAntecipada().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
