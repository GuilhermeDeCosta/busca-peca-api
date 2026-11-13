package br.com.buscapeca.buscapecaapi.usuario.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Campos que o proprio usuario pode alterar no perfil (BP1-60). */
public record AtualizarPerfilRequest(
        @Size(max = 80, message = "O nome de exibição pode ter no máximo 80 caracteres.")
        String nomeExibicao,

        @Pattern(regexp = "^(\\d{10,11})?$", message = "Informe o telefone com DDD, só números.")
        String telefone,

        @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres.")
        String descricao
) {
}
