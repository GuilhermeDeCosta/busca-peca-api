package br.com.buscapeca.buscapecaapi.loja.controller;

import br.com.buscapeca.buscapecaapi.loja.dto.CriarLojaRequest;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaPublicaResponse;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaResponse;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Minha Loja e loja publica (Documentacao do Back-end, secao 5.6). */
@RestController
@RequestMapping("/lojas")
public class LojaController {

    private final LojaService lojaService;
    private final UsuarioLogado usuarioLogado;

    public LojaController(LojaService lojaService, UsuarioLogado usuarioLogado) {
        this.lojaService = lojaService;
        this.usuarioLogado = usuarioLogado;
    }

    /** Cria a loja e da o papel VENDEDOR (BP1-67). Logado e sem loja. 201 + Location. */
    @PostMapping
    public ResponseEntity<LojaResponse> criar(@Valid @RequestBody CriarLojaRequest req) {
        LojaResponse criada = lojaService.criar(usuarioLogado.id(), req);
        URI local = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/lojas/{id}").buildAndExpand(criada.idLoja()).toUri();
        return ResponseEntity.created(local).body(criada);
    }

    /** Dados da propria loja (BP1-55). 404 se o usuario ainda nao tem loja. */
    @GetMapping("/minha")
    public LojaResponse minha() {
        return lojaService.minha(usuarioLogado.id());
    }

    /** Loja publica: logo, banner, nota, cidade e formas de pagamento (BP1-15, 55). Publico. */
    @GetMapping("/{id:\\d+}")
    public LojaPublicaResponse publica(@PathVariable Long id) {
        return lojaService.publica(id);
    }
}
