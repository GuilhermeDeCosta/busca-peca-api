package br.com.buscapeca.buscapecaapi.integracao.email;

/**
 * Envio de e-mail isolado atras de uma interface (ADR-012).
 * Os modulos de negocio conhecem so esta interface.
 */
public interface EmailService {

    /**
     * Envia o e-mail. Nunca lanca excecao: em caso de falha registra no log
     * e devolve falso, para o fluxo do usuario continuar (timeout de 10 s).
     */
    boolean enviar(String destinatario, String assunto, String texto);
}
