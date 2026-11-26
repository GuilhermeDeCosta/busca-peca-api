package br.com.buscapeca.buscapecaapi.peca.enums;

/**
 * Faixa de estoque do anuncio no painel Minha Loja (BP1-42, 52). Mesma regra do
 * nivelEstoque() do pecas.js: sem unidades, ate o limite de alerta, ou acima dele.
 */
public enum NivelEstoque {
    ZERADO,
    BAIXO,
    NORMAL;

    public static NivelEstoque de(int quantidade, int limiteAlerta) {
        if (quantidade <= 0) {
            return ZERADO;
        }
        return quantidade <= limiteAlerta ? BAIXO : NORMAL;
    }
}
