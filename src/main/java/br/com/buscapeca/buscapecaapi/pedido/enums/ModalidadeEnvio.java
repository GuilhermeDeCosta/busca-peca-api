package br.com.buscapeca.buscapecaapi.pedido.enums;

/** Como a loja envia um pedido de ENTREGA (BP1-33, V7 ck_entrega_modalidade). */
public enum ModalidadeEnvio {
    /** Entrega pela propria loja. */
    PROPRIA,
    TRANSPORTADORA,
    MOTOBOY
}
