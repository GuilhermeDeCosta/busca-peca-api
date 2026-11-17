package br.com.buscapeca.buscapecaapi.peca.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Uma linha de compatibilidade do form-peca: marca, modelo, faixa de anos e versao opcional. */
public record AplicacaoRequest(
        @NotBlank(message = "Escolha a marca do veículo.")
        @Size(max = 50, message = "Marca inválida.")
        String marca,

        @NotBlank(message = "Escolha o modelo do veículo.")
        @Size(max = 80, message = "Modelo inválido.")
        String modelo,

        @NotNull(message = "Informe o ano inicial.")
        @Min(value = 1900, message = "Ano inválido.")
        @Max(value = 2100, message = "Ano inválido.")
        Integer anoInicial,

        /** Vazio = so o ano inicial. */
        @Min(value = 1900, message = "Ano inválido.")
        @Max(value = 2100, message = "Ano inválido.")
        Integer anoFinal,

        @Size(max = 80, message = "A versão pode ter no máximo 80 caracteres.")
        String versao
) {
}
