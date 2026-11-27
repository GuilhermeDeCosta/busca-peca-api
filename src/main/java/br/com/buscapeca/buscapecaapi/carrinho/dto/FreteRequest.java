package br.com.buscapeca.buscapecaapi.carrinho.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo de POST /api/carrinho/frete (BP1-23). O CEP pode vir com ou sem hifen. */
public record FreteRequest(
        @NotBlank(message = "Informe o CEP.")
        String cep
) {
}
