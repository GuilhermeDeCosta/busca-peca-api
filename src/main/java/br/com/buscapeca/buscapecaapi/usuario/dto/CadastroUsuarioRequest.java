package br.com.buscapeca.buscapecaapi.usuario.dto;

import br.com.buscapeca.buscapecaapi.usuario.enums.TipoPessoa;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cadastro publico (BP1-2). Aceita os nomes em camelCase (padrao da API) e tambem
 * em snake_case, como o cadastro.js do front envia hoje.
 * Nao existe escolha de tipo de perfil (ADR-007);
 * o CPF/CNPJ e opcional aqui e obrigatorio so ao criar a loja (ADR-013).
 */
public record CadastroUsuarioRequest(
        @NotBlank(message = "Informe o nome.")
        @Size(max = 150, message = "O nome pode ter no máximo 150 caracteres.")
        String nome,

        @JsonAlias("nome_exibicao")
        @Size(max = 80, message = "O nome de exibição pode ter no máximo 80 caracteres.")
        String nomeExibicao,

        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 150, message = "O e-mail pode ter no máximo 150 caracteres.")
        String email,

        @NotBlank(message = "Informe a senha.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "A senha deve ter letras e números.")
        String senha,

        /** Opcional: sem valor, usa PJ quando vier CNPJ e PF nos demais casos. */
        @JsonAlias("tipo_pessoa")
        TipoPessoa tipoPessoa,

        @JsonAlias("cnpj_cpf")
        @Pattern(regexp = "^(\\d{11}|\\d{14})?$", message = "Informe o CPF (11 dígitos) ou CNPJ (14 dígitos), só números.")
        String cnpjCpf,

        @Pattern(regexp = "^(\\d{10,11})?$", message = "Informe o telefone com DDD, só números.")
        String telefone,

        @JsonAlias("aceite_termos")
        @AssertTrue(message = "É preciso aceitar os Termos de Uso e a Política de Privacidade.")
        boolean aceiteTermos
) {
}
