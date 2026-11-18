package br.com.buscapeca.buscapecaapi.busca.controller;

import br.com.buscapeca.buscapecaapi.busca.dto.BuscaFiltros;
import br.com.buscapeca.buscapecaapi.busca.dto.BuscaResposta;
import br.com.buscapeca.buscapecaapi.busca.service.BuscaService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/busca: busca publica por texto ou codigo, com filtros e ordenacao
 * (BP1-158, 150, 152). Os parametros sao os do busca.js do front.
 */
@RestController
public class BuscaController {

    private final BuscaService buscaService;

    public BuscaController(BuscaService buscaService) {
        this.buscaService = buscaService;
    }

    @GetMapping("/busca")
    public BuscaResposta buscar(@ParameterObject @ModelAttribute BuscaFiltros filtros) {
        return buscaService.buscar(filtros);
    }
}
