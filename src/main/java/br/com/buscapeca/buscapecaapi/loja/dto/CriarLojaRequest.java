package br.com.buscapeca.buscapecaapi.loja.dto;

import br.com.buscapeca.buscapecaapi.loja.enums.MetodoPagamento;
import br.com.buscapeca.buscapecaapi.usuario.dto.EnderecoRequest;
import br.com.buscapeca.buscapecaapi.usuario.enums.TipoPessoa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Corpo do POST /lojas, no formato que o criar-loja.js envia (montarRequisicao).
 * Os nomes dos campos batem com o mapa CAMPOS_API da tela, para os erros
 * aparecerem embaixo de cada campo (ex.: "endereco.cep").
 */
public record CriarLojaRequest(
        @NotBlank(message = "Informe o nome da loja.")
        @Size(min = 3, max = 100, message = "O nome da loja deve ter entre 3 e 100 caracteres.")
        String nomeLoja,

        @Size(max = 2000, message = "A descrição pode ter no máximo 2000 caracteres.")
        String descricao,

        @NotNull(message = "Escolha CPF ou CNPJ.")
        TipoPessoa tipoPessoa,

        @NotBlank(message = "Informe o CPF ou CNPJ.")
        @Pattern(regexp = "^(\\d{11}|\\d{14})$", message = "Informe o CPF (11 dígitos) ou CNPJ (14 dígitos), só números.")
        String cnpjCpf,

        @NotBlank(message = "Informe o telefone da loja.")
        @Pattern(regexp = "^\\d{10,11}$", message = "Telefone com DDD, só números.")
        String telefone,

        @NotNull(message = "Informe o endereço da loja.")
        @Valid
        EnderecoRequest endereco,

        Boolean retiradaBalcao,

        Boolean entregaCombinar,

        Boolean motoboyProprio,

        @NotEmpty(message = "Marque pelo menos uma forma de pagamento.")
        Set<MetodoPagamento> metodosPagamento,

        @AssertTrue(message = "Para criar a loja, aceite os Termos do vendedor e a Política de Privacidade.")
        boolean aceiteTermos
) {
}
