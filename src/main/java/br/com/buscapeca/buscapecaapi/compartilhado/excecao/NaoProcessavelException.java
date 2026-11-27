package br.com.buscapeca.buscapecaapi.compartilhado.excecao;

import org.springframework.http.HttpStatus;

/**
 * 422: a requisicao esta bem formada, mas o valor nao confere com o que o sistema tem
 * (ex.: CODIGO_RETIRADA_INVALIDO no balcao). O front mostra a mensagem no proprio campo.
 */
public class NaoProcessavelException extends NegocioException {

    public NaoProcessavelException(String codigo, String mensagem) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, codigo, mensagem);
    }
}
