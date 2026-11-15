package br.com.buscapeca.buscapecaapi.usuario.dto;

/** Resposta de GET /enderecos/cep/{cep}: preenche o formulario de endereco no front. */
public record CepResponse(
        String cep,
        String logradouro,
        String bairro,
        String cidade,
        String estado
) {
}