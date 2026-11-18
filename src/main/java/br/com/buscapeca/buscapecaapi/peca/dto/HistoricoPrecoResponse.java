package br.com.buscapeca.buscapecaapi.peca.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Uma linha de GET /pecas/{id}/historico-preco, no formato que o front le
 * (obterHistoricoPreco em pecas.js). precoAnterior e nulo na linha da publicacao.
 */
public record HistoricoPrecoResponse(
        LocalDateTime data,
        BigDecimal precoAnterior,
        BigDecimal precoNovo
) {
}