package br.com.buscapeca.buscapecaapi.busca.enums;

import java.util.Locale;

/** Valores do parametro "ordenar" de GET /api/busca (BP1-152). */
public enum OrdenacaoBusca {
    RELEVANCIA,
    /** Menor preco primeiro. */
    PRECO,
    /** Maior preco primeiro. */
    PRECO_DESC,
    /** Ainda sem localizacao das lojas (BP1-151): por enquanto ordena por relevancia. */
    DISTANCIA,
    /** Lojas com melhor nota primeiro. */
    AVALIACAO;

    /** Aceita o texto do front ("preco_desc"); vazio vira RELEVANCIA, invalido vira null. */
    public static OrdenacaoBusca de(String valor) {
        if (valor == null || valor.isBlank()) {
            return RELEVANCIA;
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
