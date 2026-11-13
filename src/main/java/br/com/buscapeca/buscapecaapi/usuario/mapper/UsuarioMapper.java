package br.com.buscapeca.buscapecaapi.usuario.mapper;

import br.com.buscapeca.buscapecaapi.usuario.dto.UsuarioResponse;
import br.com.buscapeca.buscapecaapi.usuario.entity.Papel;
import br.com.buscapeca.buscapecaapi.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

/**
 * Conversao entidade -> DTO. Escrito a mao por enquanto; pode ser trocado
 * por MapStruct quando a dependencia for adicionada (README, "A adicionar depois").
 */
@Component
public class UsuarioMapper {

    public UsuarioResponse paraResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNome(),
                u.getNomeExibicao(),
                u.getEmail(),
                u.getTipoPessoa(),
                u.getCnpjCpf(),
                u.getTelefone(),
                u.getFotoUrl(),
                u.getDescricao(),
                u.isIdentidadeVerificada(),
                u.getStatus(),
                u.getDataCadastro(),
                u.getPapeis().stream().map(Papel::getNome).sorted().toList());
    }
}
