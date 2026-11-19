package br.com.buscapeca.buscapecaapi.busca.repository;

import br.com.buscapeca.buscapecaapi.busca.enums.OrdenacaoBusca;
import br.com.buscapeca.buscapecaapi.loja.enums.MetodoPagamento;
import br.com.buscapeca.buscapecaapi.peca.enums.CondicaoPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.OrigemPeca;

import java.math.BigDecimal;
import java.util.List;

/**
 * Filtros da busca ja conferidos e normalizados pelo BuscaService.
 * Campo nulo (ou lista vazia) = filtro nao aplicado.
 *
 * @param textoFulltext texto no formato do MATCH ... IN BOOLEAN MODE (ex.: "+pastil* +freio*"),
 *                      ou nulo quando as palavras sao curtas demais para o indice
 * @param textoLike     texto com % para LIKE (ex.: "%pastilha bosch%")
 * @param textoCodigo   texto como codigo OEM/EAN: maiusculas, sem espaco, ponto ou hifen
 * @param textoSku      texto como SKU: so maiusculas (o SKU e gravado assim)
 * @param codigo        parametro "codigo", normalizado como OEM
 * @param codigoSku     parametro "codigo", normalizado como SKU
 */
public record CriteriosBusca(
        String textoFulltext,
        String textoLike,
        String textoCodigo,
        String textoSku,
        String codigo,
        String codigoSku,
        Integer idCategoria,
        List<Long> veiculos,
        List<Integer> tags,
        CondicaoPeca condicao,
        OrigemPeca origem,
        Boolean prontaEntrega,
        MetodoPagamento formaPagamento,
        BigDecimal notaMinima,
        Boolean retirada,
        Boolean freteGratis,
        BigDecimal precoMin,
        BigDecimal precoMax,
        OrdenacaoBusca ordenacao
) {

    public boolean temTexto() {
        return textoLike != null;
    }
}
