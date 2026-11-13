package br.com.buscapeca.buscapecaapi.catalogo.dto;

import java.util.List;

/**
 * Categoria com subcategorias. Formato usado pelo catalogo.js do front:
 * { idCategoria, nome, subcategorias: [{ idCategoria, nome, subcategorias }] }.
 */
public record CategoriaResponse(
        Integer idCategoria,
        String nome,
        Integer idCategoriaPai,
        List<CategoriaResponse> subcategorias
) {
}
