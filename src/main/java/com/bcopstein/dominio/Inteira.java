package com.bcopstein.dominio;

import java.math.BigDecimal;

/** 100% do preco-base, sem exigencias adicionais. */
public class Inteira extends Ingresso {

    public Inteira(String id, Evento evento, Participante participante) {
        super(id, evento, participante);
    }

    @Override
    public BigDecimal calcularPreco() {
        return arredondar(precoBase());
    }

    @Override
    public boolean permiteEntradaAntecipada() {
        return false;
    }

    @Override
    public Modalidade getModalidade() {
        return Modalidade.INTEIRA;
    }
}
