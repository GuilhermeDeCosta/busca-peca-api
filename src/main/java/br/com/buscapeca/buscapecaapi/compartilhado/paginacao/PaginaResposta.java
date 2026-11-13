package br.com.buscapeca.buscapecaapi.compartilhado.paginacao;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Resposta padrao das listagens: itens, total de registros e total de paginas
 * (Documentacao do Back-end, secao 2).
 */
public record PaginaResposta<T>(
        List<T> itens,
        int pagina,
        int tamanho,
        long totalRegistros,
        int totalPaginas
) {

    public static <E, T> PaginaResposta<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new PaginaResposta<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
