package br.com.buscapeca.buscapecaapi.pedido.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Codigo que o comprador mostra no balcao para retirar o pedido (BP1-29): duas letras,
 * hifen e quatro digitos (ex.: HP-3318), cabe nos 8 caracteres da coluna. Sem I e O
 * para nao confundir com 1 e 0. Aleatorio, para ninguem adivinhar o codigo de outro.
 */
@Component
public class GeradorCodigoRetirada {

    private static final String LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final SecureRandom aleatorio = new SecureRandom();

    public String gerar() {
        char a = LETRAS.charAt(aleatorio.nextInt(LETRAS.length()));
        char b = LETRAS.charAt(aleatorio.nextInt(LETRAS.length()));
        int numero = 1000 + aleatorio.nextInt(9000);
        return "" + a + b + "-" + numero;
    }
}
