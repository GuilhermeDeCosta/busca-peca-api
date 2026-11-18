package br.com.buscapeca.buscapecaapi.peca.dto;

import br.com.buscapeca.buscapecaapi.peca.enums.CondicaoPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.OrigemPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Corpo do POST /pecas e do PUT /pecas/{id}, no formato do montarDados() do form-peca.js
 * mais o status. Os nomes batem com o mapa CAMPOS_API da tela, para os erros aparecerem
 * embaixo de cada campo. O que so e exigido para publicar (categoria, OEM, preco) e
 * conferido no PecaService, porque o rascunho so precisa do titulo.
 */
public record PecaRequest(
        @NotBlank(message = "Informe o título do anúncio.")
        @Size(min = 5, max = 150, message = "O título deve ter entre 5 e 150 caracteres.")
        String titulo,

        @Size(max = 5000, message = "A descrição pode ter no máximo 5000 caracteres.")
        String descricao,

        Integer idCategoria,

        @Size(max = 60, message = "A marca pode ter no máximo 60 caracteres.")
        String marcaPeca,

        @Size(max = 60, message = "Código OEM muito longo.")
        String codigoOem,

        @Size(max = 20, message = "EAN deve ter 8, 12, 13 ou 14 números.")
        String codigoEan,

        @Size(max = 50, message = "O SKU pode ter no máximo 50 caracteres.")
        String codigoSku,

        @Size(max = 255, message = "As características podem ter no máximo 255 caracteres.")
        String caracteristicas,

        @NotNull(message = "Escolha a condição.")
        CondicaoPeca condicao,

        @NotNull(message = "Escolha a origem.")
        OrigemPeca origem,

        /** O rascunho sem preco chega como 0 (moedaParaNumero do front) e e gravado vazio. */
        @PositiveOrZero(message = "Informe um preço maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Preço inválido.")
        BigDecimal preco,

        @PositiveOrZero(message = "A quantidade não pode ser negativa.")
        @Max(value = 99999, message = "A quantidade pode ser no máximo 99999.")
        Integer quantidadeEstoque,

        Boolean prontaEntrega,

        @PositiveOrZero(message = "O prazo não pode ser negativo.")
        @Max(value = 60, message = "O prazo pode ser no máximo 60 dias.")
        Integer prazoExpedicaoDias,

        @NotNull(message = "Informe se é rascunho ou publicação.")
        StatusPeca status
) {
}
