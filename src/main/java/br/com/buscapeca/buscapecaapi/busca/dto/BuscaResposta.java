package br.com.buscapeca.buscapecaapi.busca.dto;

import java.util.List;

/**
 * Resposta de GET /api/busca. Igual a PaginaResposta, mais totalLojas
 * (quantas lojas diferentes tem resultado), que o front mostra no topo da lista.
 *
 * @param pagina comeca em 1, como o front envia
 */
public record BuscaResposta(
        List<ResultadoBusca> itens,
        int pagina,
        int tamanho,
        long totalRegistros,
        int totalPaginas,
        long totalLojas
) {
}
