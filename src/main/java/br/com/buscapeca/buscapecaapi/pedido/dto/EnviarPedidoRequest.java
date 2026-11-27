package br.com.buscapeca.buscapecaapi.pedido.dto;

import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEnvio;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo de PATCH /api/pedidos/{id}/enviar (BP1-33). Transportadora obrigatoria quando TRANSPORTADORA. */
public record EnviarPedidoRequest(
        @NotNull(message = "Informe como o pedido foi enviado.")
        ModalidadeEnvio modalidade,

        @Size(max = 80, message = "A transportadora pode ter até 80 caracteres.")
        String transportadora,

        @Size(max = 50, message = "O código de rastreio pode ter até 50 caracteres.")
        String codigoRastreio
) {
}
