package com.bcopstein.dominio;

import java.math.BigDecimal;

/** Preco-base com o desconto configurado no evento; consome a cota promocional. */
public class Promocional extends Ingresso {
    private static final BigDecimal CEM = new BigDecimal("100");

    public Promocional(String id, Evento evento, Participante participante) {
        super(id, evento, participante);
    }

    @Override
    public BigDecimal calcularPreco() {
        BigDecimal percentualPago = CEM.subtract(BigDecimal.valueOf(getEvento().getPercentualDescontoPromocional()));
        return arredondar(precoBase().multiply(percentualPago).divide(CEM));
    }

    @Override
    public boolean permiteEntradaAntecipada() {
        return false;
    }

    @Override
    public boolean consomeCotaPromocional() {
        return true;
    }

    @Override
    public Modalidade getModalidade() {
        return Modalidade.PROMOCIONAL;
    }

    @Override
    public String getDetalhe() {
        return getEvento().getPercentualDescontoPromocional() + "% de desconto";
    }
}
