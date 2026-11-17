package br.com.buscapeca.buscapecaapi.compartilhado.excecao;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 400: dado invalido verificado no servico (alem do @Valid do DTO).
 * Pode levar um campo ou varios: cada um aparece embaixo do campo na tela.
 */
public class DadosInvalidosException extends NegocioException {

    private final Map<String, String> campos;

    public DadosInvalidosException(String campo, String mensagem) {
        super(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", mensagem);
        Map<String, String> mapa = new LinkedHashMap<>();
        if (campo != null) {
            mapa.put(campo, mensagem);
        }
        this.campos = Collections.unmodifiableMap(mapa);
    }

    /** Varios campos de uma vez (campo -> mensagem), na ordem em que aparecem na tela. */
    public DadosInvalidosException(Map<String, String> campos) {
        super(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", "Verifique os campos destacados.");
        this.campos = Collections.unmodifiableMap(new LinkedHashMap<>(campos));
    }

    /** Primeiro campo com erro, ou null. */
    public String getCampo() {
        return campos.isEmpty() ? null : campos.keySet().iterator().next();
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
