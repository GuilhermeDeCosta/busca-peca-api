package br.com.buscapeca.buscapecaapi.integracao.frete;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Cotacao de frete por loja, isolada atras de uma interface (ADR-012). Os modulos de
 * negocio conhecem so esta interface. As implementacoes nunca lancam excecao: se o
 * servico falhar ou nao tiver cotacao, devolvem vazio e a loja fica com o frete a
 * combinar (timeout de 5 s, Documentacao do Back-end, secao 4).
 */
public interface FreteService {

    /**
     * Opcoes de frete de uma loja para o CEP do comprador.
     *
     * @param idLoja     loja que vai enviar
     * @param cepDestino so digitos
     * @param valorItens soma dos itens da loja no carrinho (para regras de frete gratis)
     * @return as opcoes, ou vazio quando o frete precisa ser combinado com a loja
     */
    Optional<List<OpcaoFrete>> cotar(Long idLoja, String cepDestino, BigDecimal valorItens);

    /** tipo: NORMAL ou EXPRESSO, como na tela do carrinho. */
    record OpcaoFrete(String tipo, BigDecimal valor, int prazoDias) {
    }
}
