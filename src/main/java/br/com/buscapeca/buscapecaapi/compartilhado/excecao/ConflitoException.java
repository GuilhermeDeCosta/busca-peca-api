package br.com.buscapeca.buscapecaapi.compartilhado.excecao;

import org.springframework.http.HttpStatus;

/** 409: conflito com regra de negocio (e-mail ja cadastrado, estoque insuficiente...). */
public class ConflitoException extends NegocioException {

    public ConflitoException(String mensagem) {
        super(HttpStatus.CONFLICT, "CONFLITO", mensagem);
    }
}
