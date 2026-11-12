package com.bcopstein.dominio;

/**
 * Pessoa que vai usar o ingresso (pode ser diferente do comprador).
 * O CPF e tratado como texto de 11 digitos para preservar zeros a esquerda.
 */
public record Participante(String nome, String cpf) {

    public Participante {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome do participante é obrigatório.");
        }
        if (cpf == null || !cpf.matches("\\d{11}")) {
            throw new RegraNegocioException("CPF inválido (" + cpf + "): informe exatamente 11 dígitos.");
        }
        nome = nome.trim();
    }
}
