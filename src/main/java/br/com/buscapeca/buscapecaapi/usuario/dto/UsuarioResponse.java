package br.com.buscapeca.buscapecaapi.usuario.dto;

import br.com.buscapeca.buscapecaapi.usuario.enums.StatusUsuario;
import br.com.buscapeca.buscapecaapi.usuario.enums.TipoPessoa;

import java.time.LocalDateTime;
import java.util.List;

/** Perfil do usuario. Nunca inclui senha nem hash (ADR-001). */
public record UsuarioResponse(
        Long idUsuario,
        String nome,
        String nomeExibicao,
        String email,
        TipoPessoa tipoPessoa,
        String cnpjCpf,
        String telefone,
        String fotoUrl,
        String descricao,
        boolean identidadeVerificada,
        StatusUsuario status,
        LocalDateTime dataCadastro,
        List<String> papeis
) {
}
