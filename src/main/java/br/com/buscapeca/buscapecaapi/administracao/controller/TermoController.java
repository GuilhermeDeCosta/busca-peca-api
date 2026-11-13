package br.com.buscapeca.buscapecaapi.administracao.controller;

import br.com.buscapeca.buscapecaapi.administracao.dto.TermoResponse;
import br.com.buscapeca.buscapecaapi.administracao.service.TermoUsoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/termos")
public class TermoController {

    private final TermoUsoService termoUsoService;

    public TermoController(TermoUsoService termoUsoService) {
        this.termoUsoService = termoUsoService;
    }

    /** Versao vigente dos termos e da politica (BP1-54). Publico. */
    @GetMapping("/vigentes")
    public List<TermoResponse> vigentes() {
        return termoUsoService.listarVigentes();
    }
}
