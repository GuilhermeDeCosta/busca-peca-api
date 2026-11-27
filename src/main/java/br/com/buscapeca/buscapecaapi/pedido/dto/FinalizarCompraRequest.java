package br.com.buscapeca.buscapecaapi.pedido.dto;

import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Corpo de POST /api/compras (BP1-24), como o finalizarCompra() do pedidos.js.
 *
 * @param idEndereco    endereco de entrega do comprador; obrigatorio se alguma loja for ENTREGA
 * @param lojas         uma escolha para cada loja do carrinho
 * @param totalEsperado opcional: o total que a tela mostrou. Se o preco mudou, 409 PRECO_ALTERADO
 */
public record FinalizarCompraRequest(
        Long idEndereco,

        @NotEmpty(message = "Seu carrinho está vazio.")
        List<@Valid @NotNull EscolhaLoja> lojas,

        BigDecimal totalEsperado
) {

    /** Como o comprador quer receber o pedido de uma loja. */
    public record EscolhaLoja(
            @NotNull(message = "Informe a loja.")
            Long idLoja,

            @NotNull(message = "Escolha como receber o pedido.")
            ModalidadeEntrega modalidade,

            @Size(max = 500, message = "A observação pode ter até 500 caracteres.")
            String observacao,

            boolean cienciaPagamentoDireto
    ) {
    }
}
