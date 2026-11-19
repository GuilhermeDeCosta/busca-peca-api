package br.com.buscapeca.buscapecaapi.busca.dto;

import java.math.BigDecimal;

/** Loja que aparece em cada resultado da busca. */
public record LojaResumo(
        Long idLoja,
        String nomeLoja,
        BigDecimal avaliacaoMedia,
        String cidade,
        String estado
) {
}
