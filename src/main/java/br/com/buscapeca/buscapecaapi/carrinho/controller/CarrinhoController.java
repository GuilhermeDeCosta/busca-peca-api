package br.com.buscapeca.buscapecaapi.carrinho.controller;

import br.com.buscapeca.buscapecaapi.carrinho.dto.AdicionarItemRequest;
import br.com.buscapeca.buscapecaapi.carrinho.dto.AlterarQuantidadeRequest;
import br.com.buscapeca.buscapecaapi.carrinho.dto.CarrinhoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.FreteRequest;
import br.com.buscapeca.buscapecaapi.carrinho.dto.FreteResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.ItemAdicionadoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.service.CarrinhoService;
import br.com.buscapeca.buscapecaapi.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Carrinho (Documentacao do Back-end, secao 5.7). Todas as rotas exigem login: o
 * carrinho e sempre o de quem esta logado.
 */
@RestController
@RequestMapping("/carrinho")
public class CarrinhoController {

    private final CarrinhoService carrinhoService;
    private final UsuarioLogado usuarioLogado;

    public CarrinhoController(CarrinhoService carrinhoService, UsuarioLogado usuarioLogado) {
        this.carrinhoService = carrinhoService;
        this.usuarioLogado = usuarioLogado;
    }

    /** Itens agrupados por loja, com subtotal por loja e total (BP1-22). */
    @GetMapping
    public CarrinhoResponse obter() {
        return carrinhoService.obter(usuarioLogado.id());
    }

    /** Adiciona a peca (ou soma na quantidade). 409 ESTOQUE_INSUFICIENTE; 404 se o anuncio saiu do ar. */
    @PostMapping("/itens")
    public ItemAdicionadoResponse adicionar(@Valid @RequestBody AdicionarItemRequest req) {
        return carrinhoService.adicionar(usuarioLogado.id(), req);
    }

    /** Troca a quantidade do item e devolve o carrinho recalculado. 409 ESTOQUE_INSUFICIENTE. */
    @PatchMapping("/itens/{id:\\d+}")
    public CarrinhoResponse alterarQuantidade(@PathVariable Long id, @Valid @RequestBody AlterarQuantidadeRequest req) {
        return carrinhoService.alterarQuantidade(usuarioLogado.id(), id, req);
    }

    /** Remove o item. 204; 404 se o item nao e deste carrinho. */
    @DeleteMapping("/itens/{id:\\d+}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        carrinhoService.remover(usuarioLogado.id(), id);
        return ResponseEntity.noContent().build();
    }

    /** Esvazia o carrinho. 204. */
    @DeleteMapping
    public ResponseEntity<Void> esvaziar() {
        carrinhoService.esvaziar(usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }

    /** Frete e prazo por loja a partir do CEP (BP1-23). 400 se o CEP for invalido. */
    @PostMapping("/frete")
    public FreteResponse calcularFrete(@Valid @RequestBody FreteRequest req) {
        return carrinhoService.calcularFrete(usuarioLogado.id(), req);
    }
}
