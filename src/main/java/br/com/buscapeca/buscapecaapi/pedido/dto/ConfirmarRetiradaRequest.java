package br.com.buscapeca.buscapecaapi.pedido.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de POST /api/pedidos/{id}/confirmar-retirada (BP1-29). Aceita com ou sem hifen. */
public record ConfirmarRetiradaRequest(
        @NotBlank(message = "Informe o código de retirada.")
        @Size(max = 20, message = "Código de retirada inválido.")
        String codigo
) {
}
