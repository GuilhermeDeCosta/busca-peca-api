package br.com.buscapeca.buscapecaapi.catalogo.controller;

import br.com.buscapeca.buscapecaapi.catalogo.dto.CategoriaResponse;
import br.com.buscapeca.buscapecaapi.catalogo.dto.TagResponse;
import br.com.buscapeca.buscapecaapi.catalogo.dto.VeiculoResponse;
import br.com.buscapeca.buscapecaapi.catalogo.service.CatalogoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Rotas publicas do catalogo (Documentacao do Back-end, secao 5.3).
 * /veiculos devolve uma lista simples (sem paginacao) porque o front usa
 * o resultado direto para montar os seletores de marca e modelo.
 */
@RestController
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /** Arvore de categorias e subcategorias (BP1-6). */
    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return catalogoService.arvoreDeCategorias();
    }

    /** Categoria com suas subcategorias (BP1-6). */
    @GetMapping("/categorias/{id}")
    public CategoriaResponse categoria(@PathVariable Integer id) {
        return catalogoService.categoria(id);
    }

    /** Marcas de veiculo disponiveis (BP1-4, 11). */
    @GetMapping("/veiculos/marcas")
    public List<String> marcas() {
        return catalogoService.marcasDeVeiculo();
    }

    /** Veiculos para filtro e compatibilidade; todos os filtros sao opcionais (BP1-4, 11). */
    @GetMapping("/veiculos")
    public List<VeiculoResponse> veiculos(@RequestParam(required = false) String marca,
                                          @RequestParam(required = false) String modelo,
                                          @RequestParam(required = false) Short ano) {
        return catalogoService.veiculos(marca, modelo, ano);
    }

    /** Tags usadas nos filtros (BP1-8). */
    @GetMapping("/tags")
    public List<TagResponse> tags() {
        return catalogoService.tags();
    }
}
