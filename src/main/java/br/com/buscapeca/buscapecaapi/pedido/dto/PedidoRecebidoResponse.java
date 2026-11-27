package br.com.buscapeca.buscapecaapi.pedido.dto;

import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Linha da lista de pedidos recebidos da Minha Loja (BP1-26), como os pedidosSimulados do front.
 *
 * @param comprador   nome de exibicao de quem comprou
 * @param resumoItens ex.: "1 alternador Bosch" ou "2 filtros de ar e mais 1 item"
 * @param idConversa  sempre null por enquanto: a conversa do pedido vem com o BP1-28
 */
public record PedidoRecebidoResponse(
        Long idPedido,
        String codigo,
        String comprador,
        String resumoItens,
        LocalDateTime data,
        BigDecimal valorItens,
        StatusPedido status,
        ModalidadeEntrega modalidadeEntrega,
        Long idConversa
) {
}
