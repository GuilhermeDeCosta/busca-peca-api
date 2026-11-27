package br.com.buscapeca.buscapecaapi.pedido.controller;

import br.com.buscapeca.buscapecaapi.pedido.dto.ConfirmarRetiradaRequest;
import br.com.buscapeca.buscapecaapi.pedido.dto.EnviarPedidoRequest;
import br.com.buscapeca.buscapecaapi.pedido.dto.MotivoRequest;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoDetalheResponse;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoRecebidoResponse;
import br.com.buscapeca.buscapecaapi.pedido.service.PedidoService;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.paginacao.PaginaResposta;
import br.com.buscapeca.buscapecaapi.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pedidos (Documentacao do Back-end, secao 5.8). Exige login. A loja do pedido e o
 * comprador veem o detalhe; cada mudanca de status confere quem pode fazer e devolve o
 * pedido atualizado.
 */
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final UsuarioLogado usuarioLogado;

    public PedidoController(PedidoService pedidoService, UsuarioLogado usuarioLogado) {
        this.pedidoService = pedidoService;
        this.usuarioLogado = usuarioLogado;
    }

    /** Pedidos recebidos pela loja (BP1-26). So com loja=minha; pagina comeca em 1. */
    @GetMapping
    public PaginaResposta<PedidoRecebidoResponse> listarRecebidos(
            @RequestParam(required = false) String loja,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        if (!"minha".equals(loja)) {
            throw new DadosInvalidosException("loja", "Use loja=minha para ver os pedidos recebidos.");
        }
        return pedidoService.listarRecebidos(usuarioLogado.id(), status, pagina, tamanho);
    }

    /** Detalhe para o comprador ou a loja do pedido (BP1-27). */
    @GetMapping("/{id:\\d+}")
    public PedidoDetalheResponse detalhe(@PathVariable Long id) {
        return pedidoService.detalhe(usuarioLogado.id(), id);
    }

    @PatchMapping("/{id:\\d+}/aceitar")
    public PedidoDetalheResponse aceitar(@PathVariable Long id) {
        return pedidoService.aceitar(usuarioLogado.id(), id);
    }

    @PatchMapping("/{id:\\d+}/recusar")
    public PedidoDetalheResponse recusar(@PathVariable Long id, @Valid @RequestBody MotivoRequest req) {
        return pedidoService.recusar(usuarioLogado.id(), id, req);
    }

    @PatchMapping("/{id:\\d+}/em-preparacao")
    public PedidoDetalheResponse emPreparacao(@PathVariable Long id) {
        return pedidoService.emPreparacao(usuarioLogado.id(), id);
    }

    @PatchMapping("/{id:\\d+}/pronto-retirada")
    public PedidoDetalheResponse prontoRetirada(@PathVariable Long id) {
        return pedidoService.prontoRetirada(usuarioLogado.id(), id);
    }

    /** Confere o codigo do balcao e conclui (BP1-29). 422 CODIGO_RETIRADA_INVALIDO se nao conferir. */
    @PostMapping("/{id:\\d+}/confirmar-retirada")
    public PedidoDetalheResponse confirmarRetirada(@PathVariable Long id,
                                                   @Valid @RequestBody ConfirmarRetiradaRequest req) {
        return pedidoService.confirmarRetirada(usuarioLogado.id(), id, req);
    }

    @PatchMapping("/{id:\\d+}/enviar")
    public PedidoDetalheResponse enviar(@PathVariable Long id, @Valid @RequestBody EnviarPedidoRequest req) {
        return pedidoService.enviar(usuarioLogado.id(), id, req);
    }

    /** Entrega recebida: o comprador ou a loja confirmam (BP1-27). */
    @PatchMapping("/{id:\\d+}/concluir")
    public PedidoDetalheResponse concluir(@PathVariable Long id) {
        return pedidoService.concluir(usuarioLogado.id(), id);
    }

    /** O comprador cancela enquanto aguarda o aceite (BP1-30). */
    @PatchMapping("/{id:\\d+}/cancelar")
    public PedidoDetalheResponse cancelar(@PathVariable Long id, @Valid @RequestBody MotivoRequest req) {
        return pedidoService.cancelar(usuarioLogado.id(), id, req);
    }
}
