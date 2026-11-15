package br.com.buscapeca.buscapecaapi.usuario.controller;

import br.com.buscapeca.buscapecaapi.seguranca.UsuarioLogado;
import br.com.buscapeca.buscapecaapi.usuario.dto.CepResponse;
import br.com.buscapeca.buscapecaapi.usuario.dto.EnderecoRequest;
import br.com.buscapeca.buscapecaapi.usuario.dto.EnderecoResponse;
import br.com.buscapeca.buscapecaapi.usuario.service.EnderecoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Enderecos do usuario logado (BP1-60) e consulta de CEP (BP1-7).
 * Todas as rotas exigem login; o servico confere o dono de cada endereco.
 */
@RestController
public class EnderecoController {

    private static final String BASE = "/usuarios/me/enderecos";

    private final EnderecoService enderecoService;
    private final UsuarioLogado usuarioLogado;

    public EnderecoController(EnderecoService enderecoService, UsuarioLogado usuarioLogado) {
        this.enderecoService = enderecoService;
        this.usuarioLogado = usuarioLogado;
    }

    /** Lista os enderecos, com o principal primeiro. */
    @GetMapping(BASE)
    public List<EnderecoResponse> listar() {
        return enderecoService.listar(usuarioLogado.id());
    }

    /** Detalhe de um endereco do usuario. */
    @GetMapping(BASE + "/{id}")
    public EnderecoResponse buscar(@PathVariable Long id) {
        return enderecoService.buscar(usuarioLogado.id(), id);
    }

    /** Cadastra endereco; o primeiro vira principal. 201 + Location. */
    @PostMapping(BASE)
    public ResponseEntity<EnderecoResponse> cadastrar(@Valid @RequestBody EnderecoRequest req) {
        EnderecoResponse criado = enderecoService.cadastrar(usuarioLogado.id(), req);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.idEndereco()).toUri();
        return ResponseEntity.created(local).body(criado);
    }

    /** Altera o endereco (perfil.js usa PUT). */
    @PutMapping(BASE + "/{id}")
    public EnderecoResponse atualizar(@PathVariable Long id, @Valid @RequestBody EnderecoRequest req) {
        return enderecoService.atualizar(usuarioLogado.id(), id, req);
    }

    /** Define o endereco principal. */
    @PatchMapping(BASE + "/{id}/principal")
    public EnderecoResponse definirPrincipal(@PathVariable Long id) {
        return enderecoService.definirPrincipal(usuarioLogado.id(), id);
    }

    /** Remove o endereco. 409 se for o endereco da loja. */
    @DeleteMapping(BASE + "/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        enderecoService.remover(usuarioLogado.id(), id);
        return ResponseEntity.noContent().build();
    }

    /** Rua, bairro, cidade e UF a partir do CEP (perfil.js e criar-loja.js). 404 se nao achar. */
    @GetMapping("/enderecos/cep/{cep}")
    public CepResponse consultarCep(@PathVariable String cep) {
        return enderecoService.consultarCep(cep);
    }
}
