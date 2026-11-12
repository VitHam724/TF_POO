package com.bcopstein.dominio;

/**
 * As quatro modalidades fixas. O enum serve para escolher a modalidade
 * (na tela e no CSV), agrupar relatorios e criar o objeto certo.
 * O switch aparece SO na criacao; depois disso, preco e entrada
 * antecipada sao resolvidos por polimorfismo.
 */
public enum Modalidade {
    INTEIRA("Inteira"),
    MEIA_ENTRADA("Meia-entrada"),
    VIP("VIP"),
    PROMOCIONAL("Promocional");

    private final String descricao;

    Modalidade(String descricao) {
        this.descricao = descricao;
    }

    /** Fabrica: transforma a escolha da modalidade no objeto concreto. */
    public Ingresso criarIngresso(String id, Evento evento, Participante participante, String justificativa) {
        return switch (this) {
            case INTEIRA -> new Inteira(id, evento, participante);
            case MEIA_ENTRADA -> new MeiaEntrada(id, evento, participante, justificativa);
            case VIP -> new com.bcopstein.dominio.VIP(id, evento, participante);
            case PROMOCIONAL -> new Promocional(id, evento, participante);
        };
    }

    /** Converte o texto do CSV (ex.: "MEIA_ENTRADA") na constante. */
    public static Modalidade deTexto(String texto) {
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RegraNegocioException("Modalidade desconhecida: " + texto);
        }
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
