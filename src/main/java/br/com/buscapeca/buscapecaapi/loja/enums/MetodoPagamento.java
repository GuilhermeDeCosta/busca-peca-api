package br.com.buscapeca.buscapecaapi.loja.enums;

/** Formas de pagamento aceitas pela loja (V4, loja_metodo_pagamento). No MVP, combinadas fora da plataforma. */
public enum MetodoPagamento {
    PIX,
    DINHEIRO,
    CARTAO_CREDITO,
    CARTAO_DEBITO,
    BOLETO,
    FATURADO
}
