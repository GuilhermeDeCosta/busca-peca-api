package br.com.buscapeca.buscapecaapi.carrinho.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo de POST /api/carrinho/itens (BP1-22), como o adicionarAoCarrinho() do carrinho.js.
 *
 * @param quantidade unidades a somar no carrinho; sem valor, 1
 */
public record AdicionarItemRequest(
        @NotNull(message = "Informe a peça.")
        Long idPeca,

        @Min(value = 1, message = "A quantidade deve ser pelo menos 1.")
        @Max(value = 999, message = "A quantidade máxima por item é 999.")
        Integer quantidade
) {

    public int quantidadeOuUm() {
        return quantidade == null ? 1 : quantidade;
    }
}
