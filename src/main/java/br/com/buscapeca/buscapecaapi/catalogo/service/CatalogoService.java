package br.com.buscapeca.buscapecaapi.catalogo.service;

import br.com.buscapeca.buscapecaapi.catalogo.dto.CategoriaResponse;
import br.com.buscapeca.buscapecaapi.catalogo.dto.TagResponse;
import br.com.buscapeca.buscapecaapi.catalogo.dto.VeiculoResponse;
import br.com.buscapeca.buscapecaapi.catalogo.entity.Categoria;
import br.com.buscapeca.buscapecaapi.catalogo.repository.CategoriaRepository;
import br.com.buscapeca.buscapecaapi.catalogo.repository.TagRepository;
import br.com.buscapeca.buscapecaapi.catalogo.repository.VeiculoRepository;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Catalogo tecnico: categorias, veiculos e tags (BP1-4, 6, 8, 11). */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final CategoriaRepository categoriaRepository;
    private final VeiculoRepository veiculoRepository;
    private final TagRepository tagRepository;

    public CatalogoService(CategoriaRepository categoriaRepository, VeiculoRepository veiculoRepository,
                           TagRepository tagRepository) {
        this.categoriaRepository = categoriaRepository;
        this.veiculoRepository = veiculoRepository;
        this.tagRepository = tagRepository;
    }

    /** Arvore completa: categorias principais com as subcategorias dentro. */
    public List<CategoriaResponse> arvoreDeCategorias() {
        List<Categoria> todas = categoriaRepository.findAllByOrderByNomeAsc();
        Map<Integer, List<Categoria>> filhas = todas.stream()
                .filter(c -> c.getIdCategoriaPai() != null)
                .collect(Collectors.groupingBy(Categoria::getIdCategoriaPai));
        return todas.stream()
                .filter(c -> c.getIdCategoriaPai() == null)
                .map(c -> montar(c, filhas))
                .toList();
    }

    public CategoriaResponse categoria(Integer id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
        Map<Integer, List<Categoria>> filhas = categoriaRepository.findAllByOrderByNomeAsc().stream()
                .filter(c -> c.getIdCategoriaPai() != null)
                .collect(Collectors.groupingBy(Categoria::getIdCategoriaPai));
        return montar(categoria, filhas);
    }

    public List<String> marcasDeVeiculo() {
        return veiculoRepository.listarMarcas();
    }

    public List<VeiculoResponse> veiculos(String marca, String modelo, Short ano) {
        return veiculoRepository.filtrar(vazioParaNulo(marca), vazioParaNulo(modelo), ano).stream()
                .map(VeiculoResponse::de)
                .toList();
    }

    public List<TagResponse> tags() {
        return tagRepository.findAllByOrderByNomeAsc().stream().map(TagResponse::de).toList();
    }

    private CategoriaResponse montar(Categoria c, Map<Integer, List<Categoria>> filhas) {
        List<CategoriaResponse> subcategorias = filhas.getOrDefault(c.getId(), List.of()).stream()
                .map(f -> montar(f, filhas))
                .toList();
        return new CategoriaResponse(c.getId(), c.getNome(), c.getIdCategoriaPai(), subcategorias);
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
