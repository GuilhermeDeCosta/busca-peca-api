package br.com.buscapeca.buscapecaapi.carrinho.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * GET /api/carrinho: itens agrupados por loja, com subtotal por loja (BP1-22, ADR-009).
 * Cada loja vira um pedido separado no checkout. Os valores vem calculados daqui: o front
 * so mostra. Carrinho vazio: lojas vazia e totais zerados.
 */
public record CarrinhoResponse(List<LojaCarrinhoResponse> lojas, int quantidadeTotal, BigDecimal total) {

    public record LojaCarrinhoResponse(LojaDoCarrinho loja, BigDecimal subtotal, List<ItemCarrinhoResponse> itens) {
    }

    public record LojaDoCarrinho(Long idLoja, String nomeLoja, boolean retiradaBalcao, boolean entregaCombinar,
                                 boolean motoboyProprio) {
    }

    /**
     * @param estoqueSuficiente falso se a loja tem menos unidades que o item pede, ou se o
     *                          anuncio saiu do ar: o front bloqueia o checkout
     * @param disponivel        falso se o anuncio foi pausado ou excluido, ou a loja saiu do ar
     */
    public record ItemCarrinhoResponse(Long idItem, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal,
                                       boolean estoqueSuficiente, boolean disponivel, PecaDoItem peca) {
    }

    public record PecaDoItem(Long idPeca, String titulo, String codigoOem, String codigoSku, String marcaPeca,
                             String fotoUrl, int quantidadeEstoque, boolean prontaEntrega, int prazoExpedicaoDias) {
    }
}
