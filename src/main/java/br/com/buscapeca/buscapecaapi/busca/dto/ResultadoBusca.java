package br.com.buscapeca.buscapecaapi.busca.dto;

import br.com.buscapeca.buscapecaapi.peca.enums.CondicaoPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.OrigemPeca;

import java.math.BigDecimal;

/**
 * Um anuncio no resultado da busca, no formato que o busca.js documenta.
 *
 * @param precoPromocional preco da promocao que esta valendo agora, ou nulo
 * @param fotoUrl          foto principal, ou nula enquanto a peca nao tiver fotos
 * @param favorito         sempre falso por enquanto (favoritos: BP1-155)
 * @param distanciaKm      sempre nula por enquanto (distancia: BP1-151)
 */
public record ResultadoBusca(
        Long idPeca,
        String titulo,
        String codigoOem,
        String codigoSku,
        String codigoEan,
        String marcaPeca,
        BigDecimal preco,
        BigDecimal precoPromocional,
        CondicaoPeca condicao,
        OrigemPeca origem,
        int quantidadeEstoque,
        boolean prontaEntrega,
        int prazoExpedicaoDias,
        String fotoUrl,
        boolean favorito,
        BigDecimal distanciaKm,
        LojaResumo loja
) {
}
