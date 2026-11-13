package br.com.buscapeca.buscapecaapi.seguranca.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Quem esta logado (GET /auth/me e resposta do login).
 * O cabecalho.js usa nome, nomeExibicao, papeis e temLoja.
 */
public record SessaoResponse(
        Long idUsuario,
        String nome,
        String nomeExibicao,
        String email,
        List<String> papeis,
        boolean temLoja,
        boolean administrador,
        boolean aceiteTermosPendente,
        OffsetDateTime expiraEm
) {
}
