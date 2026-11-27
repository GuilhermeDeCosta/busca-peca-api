package br.com.buscapeca.buscapecaapi.carrinho.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Corpo de PATCH /api/carrinho/itens/{id} (BP1-22): a nova quantidade, nao a diferenca. */
public record AlterarQuantidadeRequest(
        @NotNull(message = "Informe a quantidade.")
        @Min(value = 1, message = "A quantidade deve ser pelo menos 1.")
        @Max(value = 999, message = "A quantidade máxima por item é 999.")
        Integer quantidade
) {
}
