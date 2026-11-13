package br.com.buscapeca.buscapecaapi.compartilhado.excecao;

import org.springframework.http.HttpStatus;

/** 401: sem login, login invalido ou sessao expirada. */
public class NaoAutenticadoException extends NegocioException {

    public NaoAutenticadoException(String mensagem) {
        super(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", mensagem);
    }
}
