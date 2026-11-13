package br.com.buscapeca.buscapecaapi.compartilhado.excecao;

import org.springframework.http.HttpStatus;

/** 404: o registro nao existe (ou foi excluido logicamente). */
public class RecursoNaoEncontradoException extends NegocioException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", mensagem);
    }
}
