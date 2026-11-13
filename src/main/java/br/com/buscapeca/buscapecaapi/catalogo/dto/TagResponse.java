package br.com.buscapeca.buscapecaapi.catalogo.dto;

import br.com.buscapeca.buscapecaapi.catalogo.entity.Tag;

public record TagResponse(Integer idTag, String nome) {

    public static TagResponse de(Tag t) {
        return new TagResponse(t.getId(), t.getNome());
    }
}
