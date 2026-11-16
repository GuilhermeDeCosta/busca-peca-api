package br.com.buscapeca.buscapecaapi.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Campos que o proprio usuario pode alterar no perfil (BP1-60). Campo ausente nao muda. */
public record AtualizarPerfilRequest(
        @Size(max = 150, message = "Use no máximo 150 caracteres.")
        String nome,

        @Email(message = "Informe um e-mail válido.")
        @Size(max = 150, message = "O e-mail pode ter no máximo 150 caracteres.")
        String email,

        @Size(max = 80, message = "O nome de exibição pode ter no máximo 80 caracteres.")
        String nomeExibicao,

        @Pattern(regexp = "^(\\d{10,11})?$", message = "Informe o telefone com DDD, só números.")
        String telefone,

        @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres.")
        String descricao,

        @Size(max = 150, message = "A razão social pode ter no máximo 150 caracteres.")
        String razaoSocial,

        @Pattern(regexp = "^(?i)(ISENTO|[\\d./-]{2,20})?$", message = "Use só números ou \"ISENTO\".")
        String inscricaoEstadual
) {
}
