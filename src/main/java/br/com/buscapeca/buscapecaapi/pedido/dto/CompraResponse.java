package br.com.buscapeca.buscapecaapi.pedido.dto;

import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resposta de POST /api/compras e GET /api/compras/{id}: a compra com o pedido de cada
 * loja (tela de confirmacao, A09). So o comprador recebe.
 */
public record CompraResponse(
        Long idCompra,
        LocalDateTime criadaEm,
        BigDecimal total,
        Long idEndereco,
        List<PedidoDaCompra> pedidos
) {

    /**
     * @param codigo         codigo para as telas (ex.: BP-2081)
     * @param codigoRetirada so na RETIRADA; o comprador mostra no balcao (BP1-29)
     */
    public record PedidoDaCompra(
            Long idPedido,
            String codigo,
            Long idLoja,
            String nomeLoja,
            StatusPedido status,
            ModalidadeEntrega modalidade,
            String observacao,
            String codigoRetirada,
            BigDecimal subtotal,
            List<ItemDaCompra> itens
    ) {
    }

    public record ItemDaCompra(Long idPeca, String titulo, int quantidade, BigDecimal precoUnitario) {
    }
}
