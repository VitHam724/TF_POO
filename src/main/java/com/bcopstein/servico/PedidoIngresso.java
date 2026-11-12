package com.bcopstein.servico;

import com.bcopstein.dominio.Modalidade;

/**
 * Dados "crus" de um ingresso que se deseja vender, vindos da tela ou do CSV.
 * Ainda nao e um Ingresso: so vira objeto do dominio dentro de registrarVenda.
 * id == null significa "gerar automaticamente".
 */
public record PedidoIngresso(String id, Modalidade modalidade, String nome, String cpf, String justificativa) {

    public static PedidoIngresso semId(Modalidade modalidade, String nome, String cpf, String justificativa) {
        return new PedidoIngresso(null, modalidade, nome, cpf, justificativa);
    }
}
