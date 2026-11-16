package br.com.buscapeca.buscapecaapi.usuario.mapper;

import br.com.buscapeca.buscapecaapi.usuario.dto.EnderecoResponse;
import br.com.buscapeca.buscapecaapi.usuario.entity.Endereco;
import org.springframework.stereotype.Component;

/** Conversao Endereco -> EnderecoResponse. */
@Component
public class EnderecoMapper {

    public EnderecoResponse paraResponse(Endereco e) {
        return new EnderecoResponse(
                e.getId(),
                e.getIdentificacao(),
                e.getCep(),
                e.getLogradouro(),
                e.getNumero(),
                e.getComplemento(),
                e.getBairro(),
                e.getCidade(),
                e.getEstado(),
                e.getLatitude(),
                e.getLongitude(),
                e.isPrincipal());
    }
}