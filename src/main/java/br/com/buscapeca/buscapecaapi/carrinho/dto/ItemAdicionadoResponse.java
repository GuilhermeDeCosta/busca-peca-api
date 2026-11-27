package br.com.buscapeca.buscapecaapi.carrinho.dto;

/**
 * Resposta de POST /api/carrinho/itens.
 *
 * @param quantidade      unidades desta peca no carrinho depois de somar
 * @param quantidadeTotal soma das unidades do carrinho inteiro, para o contador do cabecalho
 */
public record ItemAdicionadoResponse(Long idPeca, int quantidade, int quantidadeTotal) {
}
