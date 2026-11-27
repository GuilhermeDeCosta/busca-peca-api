package br.com.buscapeca.buscapecaapi.pedido.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de PATCH /api/pedidos/{id}/recusar (loja) e /cancelar (comprador).
 *
 * @param motivo   gravado em motivo_recusa ou motivo_cancelamento
 * @param mensagem texto livre para a outra parte. Ainda nao e gravado: vai para a conversa
 *                 do pedido quando o modulo de comunicacao existir (BP1-28)
 */
public record MotivoRequest(
        @NotBlank(message = "Informe o motivo.")
        @Size(max = 255, message = "O motivo pode ter até 255 caracteres.")
        String motivo,

        @Size(max = 1000, message = "A mensagem pode ter até 1000 caracteres.")
        String mensagem
) {
}
