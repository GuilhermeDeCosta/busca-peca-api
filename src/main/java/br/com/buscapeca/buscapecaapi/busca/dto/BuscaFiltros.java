package br.com.buscapeca.buscapecaapi.busca.dto;

import java.math.BigDecimal;

/**
 * Parametros de GET /api/busca, com os mesmos nomes que o busca.js do front envia
 * (BP1-158, 150, 152). Todos sao opcionais.
 *
 * condicao, origem, formaPagamento e ordenar chegam como texto e sao conferidos no
 * BuscaService, para o erro sair em portugues no campo certo.
 */
public record BuscaFiltros(
        /** Texto livre: titulo, descricao, marca ou um codigo OEM/EAN/SKU. */
        String q,
        /** Codigo exato (OEM, EAN ou SKU). */
        String codigo,
        /** Categoria; uma categoria-pai inclui as subcategorias. */
        Integer categoria,
        /** Ids de veiculo separados por virgula (ex.: "12,13,14"). */
        String veiculo,
        /** Ids de tag separados por virgula. */
        String tags,
        /** NOVA, USADA ou RECONDICIONADA. */
        String condicao,
        /** NACIONAL ou IMPORTADA. */
        String origem,
        Boolean prontaEntrega,
        /** PIX, DINHEIRO, CARTAO_CREDITO, CARTAO_DEBITO, BOLETO ou FATURADO. */
        String formaPagamento,
        /** Nota minima da loja, de 0 a 5. */
        BigDecimal notaMinima,
        /** So lojas com retirada no balcao. */
        Boolean retirada,
        /** So lojas com raio de frete gratis. */
        Boolean freteGratis,
        /** Faixa de preco, considerando a promocao que estiver valendo. */
        BigDecimal precoMin,
        BigDecimal precoMax,
        /** Localizacao: aceitos, mas so usados quando houver distancia (BP1-151). */
        String cep,
        BigDecimal lat,
        BigDecimal lng,
        BigDecimal raioKm,
        /** relevancia (padrao), preco, preco_desc, distancia ou avaliacao. */
        String ordenar,
        /** Comeca em 1, como no front (combinado com o Gabriel em 17/11/2026). */
        Integer pagina,
        Integer tamanho
) {
}
