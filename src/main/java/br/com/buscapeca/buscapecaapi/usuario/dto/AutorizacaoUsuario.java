package br.com.buscapeca.buscapecaapi.usuario.dto;

import br.com.buscapeca.buscapecaapi.usuario.enums.StatusUsuario;

import java.util.Set;

/**
 * Dados que a seguranca precisa a cada requisicao: status da conta,
 * papeis e permissoes, sempre lidos do banco (revogacao imediata, ADR-006).
 */
public record AutorizacaoUsuario(
        Long id,
        StatusUsuario status,
        Set<String> papeis,
        Set<String> permissoes
) {
}
