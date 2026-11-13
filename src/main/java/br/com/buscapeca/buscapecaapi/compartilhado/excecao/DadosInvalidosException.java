package br.com.buscapeca.buscapecaapi.compartilhado.excecao;

import org.springframework.http.HttpStatus;

/** 400: dado invalido verificado no servico (alem do @Valid do DTO). */
public class DadosInvalidosException extends NegocioException {

    private final String campo;

    public DadosInvalidosException(String campo, String mensagem) {
        super(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
