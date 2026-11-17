package br.com.buscapeca.buscapecaapi.peca.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Corpo do PUT /pecas/{id}/compatibilidades: a lista inteira, que substitui a anterior. */
public record CompatibilidadesRequest(
        @NotNull(message = "Informe as aplicações.")
        @Size(max = 50, message = "Use no máximo 50 aplicações.")
        List<@Valid AplicacaoRequest> aplicacoes
) {
}
