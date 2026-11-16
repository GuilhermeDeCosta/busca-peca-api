package br.com.buscapeca.buscapecaapi.loja.dto;

import br.com.buscapeca.buscapecaapi.loja.enums.MetodoPagamento;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pagina publica da loja (GET /lojas/{id}, BP1-15, 55). Sem documento,
 * telefone e endereco completo do dono (ADR-013): so cidade e UF.
 */
public record LojaPublicaResponse(
        Long idLoja,
        String nomeLoja,
        String descricao,
        String logoUrl,
        String bannerUrl,
        BigDecimal avaliacaoMedia,
        boolean vendedorVerificado,
        String cidade,
        String estado,
        boolean retiradaBalcao,
        String horarioRetirada,
        boolean entregaCombinar,
        boolean motoboyProprio,
        boolean entregaRapida,
        BigDecimal freteGratisRaioKm,
        BigDecimal pedidoMinimoFreteGratis,
        boolean aceitaFaturamentoCnpj,
        List<MetodoPagamento> metodosPagamento
) {
}
