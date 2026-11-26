package br.com.buscapeca.buscapecaapi.peca.dto;

/**
 * Parametros de GET /api/pecas?loja=minha, com os nomes que o listarMeusAnuncios() do
 * pecas.js envia (BP1-225). Todos opcionais, menos loja.
 *
 * status e estoque chegam como texto e sao conferidos no PecaService, para o erro sair
 * em portugues no campo certo (mesmo jeito da busca).
 */
public record MeusAnunciosFiltros(
        /** Sempre "minha": a listagem e so da loja de quem esta logado. */
        String loja,
        /** Parte do titulo, do SKU ou do codigo OEM. */
        String q,
        /** ATIVO, PAUSADO ou RASCUNHO. Vazio traz os tres; excluidos nunca aparecem. */
        String status,
        /** ZERADO, BAIXO (ate o limite de alerta) ou NORMAL. */
        String estoque,
        /** Comeca em 1, como no front (igual a busca). */
        Integer pagina,
        /** Padrao 20, maximo 100. */
        Integer tamanho
) {
}
