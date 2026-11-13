package br.com.buscapeca.buscapecaapi.seguranca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpo do POST /auth/redefinir-senha (Redefinir_senha.js). */
public record RedefinirSenhaRequest(
        @NotBlank(message = "Este link é inválido ou expirou. Peça um novo link de recuperação.")
        String token,

        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, max = 72, message = "Use pelo menos 8 caracteres, incluindo letras e números.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Use pelo menos 8 caracteres, incluindo letras e números.")
        String novaSenha,

        /** Opcional; quando vier, precisa ser igual a novaSenha. */
        String confirmacaoSenha
) {
}
