package br.com.buscapeca.buscapecaapi.peca.controller;

import br.com.buscapeca.buscapecaapi.compartilhado.paginacao.PaginaResposta;
import br.com.buscapeca.buscapecaapi.peca.dto.CompatibilidadesRequest;
import br.com.buscapeca.buscapecaapi.peca.dto.CompatibilidadesResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.HistoricoPrecoResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.MeuAnuncioResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.MeusAnunciosFiltros;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaRequest;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.StatusAnuncioResponse;
import br.com.buscapeca.buscapecaapi.peca.service.PecaService;
import br.com.buscapeca.buscapecaapi.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Pecas (Documentacao do Back-end, secao 5.5): cadastro, edicao, detalhe, compatibilidade,
 * historico de preco e o gerenciamento dos anuncios no painel Minha Loja.
 */
@RestController
@RequestMapping("/pecas")
public class PecaController {

    private final PecaService pecaService;
    private final UsuarioLogado usuarioLogado;

    public PecaController(PecaService pecaService, UsuarioLogado usuarioLogado) {
        this.pecaService = pecaService;
        this.usuarioLogado = usuarioLogado;
    }

    /**
     * Anuncios da propria loja, com filtros e paginacao (BP1-225). So responde com
     * loja=minha; a pagina comeca em 1. 403 se a conta ainda nao tem loja.
     */
    @GetMapping(params = "loja=minha")
    public PaginaResposta<MeuAnuncioResponse> listarMinhas(@ParameterObject @ModelAttribute MeusAnunciosFiltros filtros) {
        return pecaService.listarMinhas(usuarioLogado.id(), filtros);
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

    /** Pausa o anuncio ativo (BP1-226). 409 STATUS_INVALIDO se nao estiver ativo. So a loja dona. */
    @PatchMapping("/{id:\\d+}/pausar")
    public StatusAnuncioResponse pausar(@PathVariable Long id) {
        return pecaService.pausar(usuarioLogado.id(), id);
    }

    /** Reativa o anuncio pausado (BP1-226). 409 STATUS_INVALIDO ou ESTOQUE_ZERADO. So a loja dona. */
    @PatchMapping("/{id:\\d+}/reativar")
    public StatusAnuncioResponse reativar(@PathVariable Long id) {
        return pecaService.reativar(usuarioLogado.id(), id);
    }

    /**
     * Exclusao logica do anuncio ou rascunho (BP1-227). 204 sem corpo; 404 se ja foi
     * excluido; 409 PEDIDOS_EM_ANDAMENTO se a peca esta em pedido que ainda vai ser entregue.
     */
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        pecaService.excluir(usuarioLogado.id(), id);
        return ResponseEntity.noContent().build();
    }

    /** Historico de preco do anuncio (BP1-53), do mais recente para a publicacao. So a loja dona. */
    @GetMapping("/{id:\\d+}/historico-preco")
    public List<HistoricoPrecoResponse> historicoPreco(@PathVariable Long id) {
        return pecaService.historicoPreco(usuarioLogado.id(), id);
    }
}