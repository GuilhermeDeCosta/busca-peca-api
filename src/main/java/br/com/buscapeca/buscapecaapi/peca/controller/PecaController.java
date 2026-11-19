package br.com.buscapeca.buscapecaapi.peca.controller;

import br.com.buscapeca.buscapecaapi.peca.dto.CompatibilidadesRequest;
import br.com.buscapeca.buscapecaapi.peca.dto.CompatibilidadesResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.HistoricoPrecoResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaRequest;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaResponse;
import br.com.buscapeca.buscapecaapi.peca.service.PecaService;
import br.com.buscapeca.buscapecaapi.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Pecas (Documentacao do Back-end, secao 5.5): cadastro, edicao, detalhe, compatibilidade e historico de preco. */
@RestController
@RequestMapping("/pecas")
public class PecaController {

    private final PecaService pecaService;
    private final UsuarioLogado usuarioLogado;

    public PecaController(PecaService pecaService, UsuarioLogado usuarioLogado) {
        this.pecaService = pecaService;
        this.usuarioLogado = usuarioLogado;
    }

    /** Cadastra a peca como rascunho ou publicada (BP1-3, 45). Precisa ter loja. 201 + Location. */
    @PostMapping
    public ResponseEntity<PecaResponse> criar(@Valid @RequestBody PecaRequest req) {
        PecaResponse criada = pecaService.criar(usuarioLogado.id(), req);
        URI local = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/pecas/{id}").buildAndExpand(criada.idPeca()).toUri();
        return ResponseEntity.created(local).body(criada);
    }

    /** Edita os dados da peca (BP1-44). So a loja dona. */
    @PutMapping("/{id:\\d+}")
    public PecaResponse atualizar(@PathVariable Long id, @Valid @RequestBody PecaRequest req) {
        return pecaService.atualizar(usuarioLogado.id(), id, req);
    }

    /** Detalhe (BP1-4). Publico para anuncios ativos; o dono ve tambem rascunho e pausado. */
    @GetMapping("/{id:\\d+}")
    public PecaResponse obter(@PathVariable Long id) {
        return pecaService.obter(id, usuarioLogado.idSeLogado());
    }

    /** Define os veiculos compativeis, substituindo a lista anterior (BP1-45). So a loja dona. */
    @PutMapping("/{id:\\d+}/compatibilidades")
    public CompatibilidadesResponse definirCompatibilidades(@PathVariable Long id,
                                                            @Valid @RequestBody CompatibilidadesRequest req) {
        return pecaService.definirCompatibilidades(usuarioLogado.id(), id, req);
    }

    /** Historico de preco do anuncio (BP1-53), do mais recente para a publicacao. So a loja dona. */
    @GetMapping("/{id:\\d+}/historico-preco")
    public List<HistoricoPrecoResponse> historicoPreco(@PathVariable Long id) {
        return pecaService.historicoPreco(usuarioLogado.id(), id);
    }
}