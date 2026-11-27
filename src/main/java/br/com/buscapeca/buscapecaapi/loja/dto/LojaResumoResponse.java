package br.com.buscapeca.buscapecaapi.loja.dto;

/**
 * O pouco da loja que o carrinho e o checkout mostram (BP1-22, 24). motoboyProprio e a
 * coluna entrega_rapida (V14), com o nome que o front usa.
 *
 * @param ativa loja inativa nao vende: seus itens ficam indisponiveis no carrinho
 */
public record LojaResumoResponse(
        Long idLoja,
        String nomeLoja,
        boolean retiradaBalcao,
        boolean entregaCombinar,
        boolean motoboyProprio,
        boolean ativa
) {
}
