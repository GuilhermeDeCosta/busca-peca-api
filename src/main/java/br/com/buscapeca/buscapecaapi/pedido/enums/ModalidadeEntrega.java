package br.com.buscapeca.buscapecaapi.pedido.enums;

/** Como o comprador recebe o pedido de cada loja (BP1-24). */
public enum ModalidadeEntrega {
    /** Retirada no balcao da loja, com codigo de retirada (BP1-29). */
    RETIRADA,
    /** Entrega combinada com a loja (motoboy proprio ou a combinar). */
    ENTREGA
}
