package br.com.buscapeca.buscapecaapi.peca.dto;

import java.math.BigDecimal;

/**
 * Dados da peca que o carrinho e o checkout precisam (BP1-22, 24), entregues pelo
 * PecaCompraService para os outros modulos nao lerem a tabela peca (ADR-003).
 *
 * @param preco      preco que o comprador paga agora: o promocional vigente ou o normal
 * @param fotoUrl    foto principal, ou null enquanto o anuncio nao tem fotos
 * @param disponivel anuncio ATIVO; rascunho, pausado e excluido nao podem ser comprados
 */
public record PecaParaCompra(
        Long idPeca,
        Long idLoja,
        String titulo,
        String codigoOem,
        String codigoSku,
        String marcaPeca,
        String fotoUrl,
        BigDecimal preco,
        int quantidadeEstoque,
        boolean prontaEntrega,
        int prazoExpedicaoDias,
        boolean disponivel
) {
}
