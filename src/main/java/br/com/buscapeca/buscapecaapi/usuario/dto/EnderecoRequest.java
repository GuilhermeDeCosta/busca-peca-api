package br.com.buscapeca.buscapecaapi.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpo do POST e do PUT de /usuarios/me/enderecos (BP1-60). */
public record EnderecoRequest(
        @Size(max = 40, message = "A identificação pode ter no máximo 40 caracteres.")
        String identificacao,

        @NotBlank(message = "Informe o CEP.")
        @Pattern(regexp = "^\\d{8}$", message = "Informe um CEP com 8 números.")
        String cep,

        @NotBlank(message = "Informe a rua.")
        @Size(max = 150, message = "A rua pode ter no máximo 150 caracteres.")
        String logradouro,

        @NotBlank(message = "Informe o número ou S/N.")
        @Size(max = 10, message = "O número pode ter no máximo 10 caracteres.")
        String numero,

        @Size(max = 80, message = "O complemento pode ter no máximo 80 caracteres.")
        String complemento,

        @NotBlank(message = "Informe o bairro.")
        @Size(max = 80, message = "O bairro pode ter no máximo 80 caracteres.")
        String bairro,

        @NotBlank(message = "Informe a cidade.")
        @Size(max = 80, message = "A cidade pode ter no máximo 80 caracteres.")
        String cidade,

        @NotBlank(message = "Escolha a UF.")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "Escolha a UF.")
        String estado,

        /** Opcional. Se vier true, este vira o endereço principal. */
        Boolean principal
) {
}