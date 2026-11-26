package br.com.buscapeca.buscapecaapi.peca.dto;

import br.com.buscapeca.buscapecaapi.peca.enums.CondicaoPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.NivelEstoque;
import br.com.buscapeca.buscapecaapi.peca.enums.OrigemPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Linha da tabela "Meus anuncios" do painel.js (BP1-225). Os codigos vazios vem como ""
 * em vez de null, porque a tela monta o texto "titulo • codigoSku" direto.
 *
 * @param preco         nulo em rascunho sem preco
 * @param nivelEstoque  ZERADO, BAIXO ou NORMAL, ja calculado com o limite de alerta
 */
public record MeuAnuncioResponse(
        Long idPeca,
        String titulo,
        String codigoSku,
        String codigoOem,
        BigDecimal preco,
        int quantidadeEstoque,
        int limiteAlertaEstoque,
        NivelEstoque nivelEstoque,
        CondicaoPeca condicao,
        OrigemPeca origem,
        boolean prontaEntrega,
        int prazoExpedicaoDias,
        StatusPeca status,
        LocalDateTime dataCadastro,
        LocalDateTime atualizadoEm
) {
}
