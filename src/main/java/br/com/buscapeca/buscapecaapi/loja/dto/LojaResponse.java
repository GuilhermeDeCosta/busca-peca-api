package br.com.buscapeca.buscapecaapi.loja.dto;

import br.com.buscapeca.buscapecaapi.loja.enums.MetodoPagamento;
import br.com.buscapeca.buscapecaapi.usuario.dto.EnderecoResponse;
import br.com.buscapeca.buscapecaapi.usuario.enums.TipoPessoa;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A propria loja (POST /lojas e GET /lojas/minha), no formato da lojaSimulada
 * do front (simulacao.js): o painel e a tela de configuracoes leem estes campos.
 */
public record LojaResponse(
        Long idLoja,
        String nomeLoja,
        String descricao,
        String telefone,
        TipoPessoa tipoPessoa,
        String cnpjCpf,
        EnderecoResponse endereco,
        String cidade,
        String estado,
        String logoUrl,
        String bannerUrl,
        boolean ativa,
        BigDecimal avaliacaoMedia,
        boolean vendedorVerificado,
        boolean retiradaBalcao,
        String horarioRetirada,
        boolean entregaCombinar,
        boolean motoboyProprio,
        boolean entregaRapida,
        BigDecimal freteGratisRaioKm,
        BigDecimal pedidoMinimoFreteGratis,
        boolean aceitaFaturamentoCnpj,
        List<MetodoPagamento> metodosPagamento,
        String nomeDono,
        String nomeOficina,
        LocalDateTime atualizadoEm
) {
}
