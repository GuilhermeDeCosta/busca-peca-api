package br.com.buscapeca.buscapecaapi.compartilhado.excecao;

import org.springframework.http.HttpStatus;

/**
 * Base das excecoes de negocio. O tratador global transforma cada uma
 * na resposta de erro padrao (Documentacao do Back-end, secao 6).
 */
public abstract class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    protected NegocioException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
