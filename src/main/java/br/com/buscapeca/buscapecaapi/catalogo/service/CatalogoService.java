package br.com.buscapeca.buscapecaapi.catalogo.service;

import br.com.buscapeca.buscapecaapi.catalogo.dto.CategoriaResponse;
import br.com.buscapeca.buscapecaapi.catalogo.dto.TagResponse;
import br.com.buscapeca.buscapecaapi.catalogo.dto.VeiculoResponse;
import br.com.buscapeca.buscapecaapi.catalogo.entity.Categoria;
import br.com.buscapeca.buscapecaapi.catalogo.entity.Veiculo;
import br.com.buscapeca.buscapecaapi.catalogo.repository.CategoriaRepository;
import br.com.buscapeca.buscapecaapi.catalogo.repository.TagRepository;
import br.com.buscapeca.buscapecaapi.catalogo.repository.VeiculoRepository;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
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

    /** Usado pelo modulo de pecas para conferir a categoria escolhida. */
    public boolean categoriaExiste(Integer idCategoria) {
        return idCategoria != null && categoriaRepository.existsById(idCategoria);
    }

    /**
     * Id do veiculo marca/modelo/ano/versao, criando a linha se esse ano ou versao ainda
     * nao existir. Marca e modelo precisam estar no catalogo (o form-peca so oferece esses);
     * ficam com a grafia do catalogo. Usado na compatibilidade da peca (BP1-45).
     */
    @Transactional
    public Long obterOuCriarVeiculo(String marca, String modelo, short ano, String versao) {
        Veiculo referencia = veiculoRepository.findFirstByMarcaIgnoreCaseAndModeloIgnoreCase(marca.trim(), modelo.trim())
                .orElseThrow(() -> new DadosInvalidosException("aplicacoes",
                        "O veículo " + marca.trim() + " " + modelo.trim() + " não está no catálogo."));
        String versaoLimpa = vazioParaNulo(versao);
        return veiculoRepository.buscar(referencia.getMarca(), referencia.getModelo(), ano, versaoLimpa)
                .map(Veiculo::getId)
                .orElseGet(() -> veiculoRepository.save(
                        new Veiculo(referencia.getMarca(), referencia.getModelo(), ano, versaoLimpa)).getId());
    }

    public List<VeiculoResponse> veiculosPorIds(Collection<Long> ids) {
        return veiculoRepository.findAllById(ids).stream().map(VeiculoResponse::de).toList();
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
