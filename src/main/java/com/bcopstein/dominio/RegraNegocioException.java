package com.bcopstein.dominio;

/**
 * Lancada quando uma operacao viola uma regra de negocio
 * (capacidade, cota promocional, evento passado, CPF repetido etc.).
 * A interface captura esta excecao e mostra a mensagem ao usuario.
 */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
