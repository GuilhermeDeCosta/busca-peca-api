package br.com.buscapeca.buscapecaapi.pedido.enums;

/**
 * Status do pedido (ADR-009, V7 e V15): AGUARDANDO_ACEITE -> ACEITO ou RECUSADO ->
 * EM_PREPARACAO -> PRONTO_RETIRADA ou EM_ENTREGA -> CONCLUIDO. CANCELADO antes do envio.
 */
public enum StatusPedido {
    AGUARDANDO_ACEITE,
    ACEITO,
    RECUSADO,
    EM_PREPARACAO,
    PRONTO_RETIRADA,
    EM_ENTREGA,
    CONCLUIDO,
    CANCELADO
}
