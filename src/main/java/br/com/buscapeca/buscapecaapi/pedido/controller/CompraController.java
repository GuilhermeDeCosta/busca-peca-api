package br.com.buscapeca.buscapecaapi.pedido.controller;

import br.com.buscapeca.buscapecaapi.pedido.dto.CompraResponse;
import br.com.buscapeca.buscapecaapi.pedido.dto.FinalizarCompraRequest;
import br.com.buscapeca.buscapecaapi.pedido.service.CompraService;
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

/** Compras (Documentacao do Back-end, secao 5.8): checkout e detalhe da compra. Exige login. */
@RestController
@RequestMapping("/compras")
public class CompraController {

    private final CompraService compraService;
    private final UsuarioLogado usuarioLogado;

    public CompraController(CompraService compraService, UsuarioLogado usuarioLogado) {
        this.compraService = compraService;
        this.usuarioLogado = usuarioLogado;
    }

    /** Checkout: cria a compra e um pedido por loja a partir do carrinho (BP1-24). 201 + Location. */
    @PostMapping
    public ResponseEntity<CompraResponse> finalizar(@Valid @RequestBody FinalizarCompraRequest req) {
        CompraResponse compra = compraService.finalizar(usuarioLogado.id(), req);
        URI local = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/compras/{id}").buildAndExpand(compra.idCompra()).toUri();
        return ResponseEntity.created(local).body(compra);
    }

    /** A compra com o pedido de cada loja (tela de confirmacao). So o comprador. */
    @GetMapping("/{id:\\d+}")
    public CompraResponse obter(@PathVariable Long id) {
        return compraService.obter(usuarioLogado.id(), id);
    }
}
