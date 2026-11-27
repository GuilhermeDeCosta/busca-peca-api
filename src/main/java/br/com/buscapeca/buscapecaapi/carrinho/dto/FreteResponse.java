package br.com.buscapeca.buscapecaapi.carrinho.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * POST /api/carrinho/frete: frete e prazo por loja do carrinho (BP1-23).
 * Sem cotacao, a loja volta com freteACombinar = true e opcoes vazia (ADR-012).
 *
 * @param regiao cidade e UF do CEP; vazias quando a consulta de CEP nao encontrou
 */
public record FreteResponse(Regiao regiao, List<LojaFrete> lojas) {

    public record Regiao(String cidade, String uf) {
    }

    public record LojaFrete(Long idLoja, String nomeLoja, boolean retiradaBalcao, boolean freteACombinar,
                            List<OpcaoFrete> opcoes) {
    }

    /** tipo: NORMAL ou EXPRESSO. */
    public record OpcaoFrete(String tipo, BigDecimal valor, int prazoDias) {
    }
}
