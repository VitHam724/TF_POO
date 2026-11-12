package com.bcopstein.dominio;

import java.math.BigDecimal;

/** 50% do preco-base; exige registrar a justificativa (estudante, idoso...). */
public class MeiaEntrada extends Ingresso {
    private static final BigDecimal FATOR = new BigDecimal("0.50");
    private final String justificativa;

    public MeiaEntrada(String id, Evento evento, Participante participante, String justificativa) {
        super(id, evento, participante);
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraNegocioException("Meia-entrada exige justificativa (ex.: estudante, idoso).");
        }
        this.justificativa = justificativa.trim();
    }

    @Override
    public BigDecimal calcularPreco() {
        return arredondar(precoBase().multiply(FATOR));
    }

    @Override
    public boolean permiteEntradaAntecipada() {
        return false;
    }

    @Override
    public Modalidade getModalidade() {
        return Modalidade.MEIA_ENTRADA;
    }

    @Override
    public String getDetalhe() {
        return "Justificativa: " + justificativa;
    }

    public String getJustificativa() {
        return justificativa;
    }
}
