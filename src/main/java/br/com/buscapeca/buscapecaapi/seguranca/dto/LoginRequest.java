package br.com.buscapeca.buscapecaapi.seguranca.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corpo do POST /auth/login (login.js envia email, senha e manterConectado). */
public record LoginRequest(
        @NotBlank(message = "Informe seu e-mail.")
        @Email(message = "Informe um e-mail válido.")
        String email,

        @NotBlank(message = "Informe sua senha.")
        String senha,

        /** Verdadeiro: o cookie sobrevive ao fechar o navegador (ate o JWT vencer). */
        Boolean manterConectado
) {
}
