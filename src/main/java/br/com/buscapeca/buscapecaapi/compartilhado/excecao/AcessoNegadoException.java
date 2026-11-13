package br.com.buscapeca.buscapecaapi.compartilhado.excecao;

import org.springframework.http.HttpStatus;

/** 403: logado, mas sem permissao (ex.: editar a peca de outra loja). */
public class AcessoNegadoException extends NegocioException {

    public AcessoNegadoException(String mensagem) {
        super(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", mensagem);
    }
}
