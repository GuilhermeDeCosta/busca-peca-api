package br.com.buscapeca.buscapecaapi.peca.dto;

import br.com.buscapeca.buscapecaapi.peca.enums.CondicaoPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.OrigemPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Peca no formato que o form-peca.js carrega (obterPecaParaEdicao). Os textos opcionais
 * vem como "" em vez de null, porque a tela chama .trim() neles ao salvar de novo.
 */
public record PecaResponse(
        Long idPeca,
        Long idLoja,
        String titulo,
        String descricao,
        Integer idCategoria,
        String marcaPeca,
        String codigoOem,
        String codigoEan,
        String codigoSku,
        String caracteristicas,
        CondicaoPeca condicao,
        OrigemPeca origem,
        BigDecimal preco,
        int quantidadeEstoque,
        boolean prontaEntrega,
        int prazoExpedicaoDias,
        StatusPeca status,
        LocalDateTime dataCadastro,
        LocalDateTime atualizadoEm,
        List<AplicacaoResponse> aplicacoes
) {
}
