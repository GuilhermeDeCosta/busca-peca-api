package br.com.buscapeca.buscapecaapi.usuario.dto;

import java.math.BigDecimal;

/** Endereco devolvido pela API (BP1-60). */
public record EnderecoResponse(
        Long idEndereco,
        String identificacao,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean principal
) {
}