package br.com.buscapeca.buscapecaapi.peca.dto;

import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;

import java.time.LocalDateTime;

/** Resposta de pausar e reativar (BP1-226), no formato que o pecas.js espera. */
public record StatusAnuncioResponse(
        Long idPeca,
        StatusPeca status,
        LocalDateTime atualizadoEm
) {
}
