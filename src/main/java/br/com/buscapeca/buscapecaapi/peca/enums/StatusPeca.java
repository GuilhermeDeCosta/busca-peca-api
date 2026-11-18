package br.com.buscapeca.buscapecaapi.peca.enums;

/**
 * Situacao do anuncio (V12). So ATIVO aparece na busca.
 * EXCLUIDO e a exclusao logica: a peca some da loja, mas pedidos antigos continuam apontando para ela.
 */
public enum StatusPeca {
    RASCUNHO,
    ATIVO,
    PAUSADO,
    EXCLUIDO;

    /** Ativo ou pausado: ja passou pela publicacao e tem os campos obrigatorios. */
    public boolean publicado() {
        return this == ATIVO || this == PAUSADO;
    }
}
