package br.com.buscapeca.buscapecaapi.usuario.controller;

import br.com.buscapeca.buscapecaapi.administracao.service.TermoUsoService;
import br.com.buscapeca.buscapecaapi.seguranca.UsuarioLogado;
import br.com.buscapeca.buscapecaapi.usuario.dto.AtualizarPerfilRequest;
import br.com.buscapeca.buscapecaapi.usuario.dto.CadastroUsuarioRequest;
import br.com.buscapeca.buscapecaapi.usuario.dto.TrocarSenhaRequest;
import br.com.buscapeca.buscapecaapi.usuario.dto.UsuarioResponse;
import br.com.buscapeca.buscapecaapi.usuario.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioLogado usuarioLogado;
    private final TermoUsoService termoUsoService;

    public UsuarioController(UsuarioService usuarioService, UsuarioLogado usuarioLogado,
                             TermoUsoService termoUsoService) {
        this.usuarioService = usuarioService;
        this.usuarioLogado = usuarioLogado;
        this.termoUsoService = termoUsoService;
    }

    /** Cadastro com aceite dos termos; a conta recebe o papel COMPRADOR (BP1-2). Publico. */
    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroUsuarioRequest req) {
        UsuarioResponse criado = usuarioService.cadastrar(req);
        URI local = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/usuarios/me").build().toUri();
        return ResponseEntity.created(local).body(criado);
    }

    /** Dados do perfil (BP1-60). */
    @GetMapping("/me")
    public UsuarioResponse perfil() {
        return usuarioService.buscarPerfil(usuarioLogado.id());
    }

    /** Altera nome de exibicao, telefone e descricao (BP1-60). Aceita PUT e PATCH (perfil.js usa PATCH). */
    @RequestMapping(path = "/me", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public UsuarioResponse atualizar(@Valid @RequestBody AtualizarPerfilRequest req) {
        return usuarioService.atualizarPerfil(usuarioLogado.id(), req);
    }

    /** Troca a senha informando a atual (BP1-60). */
    @PutMapping("/me/senha")
    public ResponseEntity<Void> trocarSenha(@Valid @RequestBody TrocarSenhaRequest req) {
        usuarioService.trocarSenha(usuarioLogado.id(), req);
        return ResponseEntity.noContent().build();
    }

    /** Aceita a versao vigente dos termos, quando uma nova e publicada (BP1-54). */
    @PostMapping("/me/termos/aceite")
    public ResponseEntity<Void> aceitarTermos() {
        termoUsoService.registrarAceiteDosVigentes(usuarioLogado.id());
        return ResponseEntity.noContent().build();
    }
}
