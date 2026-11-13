package br.com.buscapeca.buscapecaapi.compartilhado.paginacao;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Converte os parametros "pagina" e "tamanho" da API em Pageable.
 * Padrao de 20 itens e maximo de 100 (ADR-001).
 */
public final class Paginacao {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    private Paginacao() {
    }

    public static Pageable de(Integer pagina, Integer tamanho, Sort ordem) {
        int p = pagina == null || pagina < 0 ? 0 : pagina;
        int t = tamanho == null || tamanho < 1 ? TAMANHO_PADRAO : Math.min(tamanho, TAMANHO_MAXIMO);
        return PageRequest.of(p, t, ordem);
    }
}
