package br.com.buscapeca.buscapecaapi.seguranca;

import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.seguranca.dto.CsrfResponse;
import br.com.buscapeca.buscapecaapi.seguranca.dto.LoginRequest;
import br.com.buscapeca.buscapecaapi.seguranca.dto.RecuperarSenhaRequest;
import br.com.buscapeca.buscapecaapi.seguranca.dto.RedefinirSenhaRequest;
import br.com.buscapeca.buscapecaapi.seguranca.dto.SessaoResponse;
import br.com.buscapeca.buscapecaapi.usuario.service.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas de /api/auth (Documentacao do Back-end, secao 5.1). */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;
    private final CookieSessao cookieSessao;

    public AuthController(AuthService authService, RecuperacaoSenhaService recuperacaoSenhaService,
                          CookieSessao cookieSessao) {
        this.authService = authService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
        this.cookieSessao = cookieSessao;
    }

    /** Confere e-mail e senha e grava o JWT no cookie HttpOnly (BP1-41). Publico. */
    @PostMapping("/login")
    public ResponseEntity<SessaoResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthService.Login login = authService.entrar(req.email(), req.senha());
        boolean manter = Boolean.TRUE.equals(req.manterConectado());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        cookieSessao.criar(login.token().valor(), login.token().validade(), manter).toString())
                .body(login.sessao());
    }

    /** Apaga o cookie de sessao. Sempre responde 204. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieSessao.apagar().toString())
                .build();
    }

    /** Usuario logado, papeis e se tem loja. Cada pagina chama ao abrir. */
    @GetMapping("/me")
    public SessaoResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.sessaoAtual(Long.valueOf(jwt.getSubject()), jwt.getExpiresAt());
    }

    /** Token CSRF para POST, PUT, PATCH e DELETE (cabecalho X-XSRF-TOKEN). Publico. */
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    /** Envia o e-mail com o link de uso unico (BP1-40). 404 se o e-mail nao tem conta. Publico. */
    @PostMapping("/recuperar-senha")
    public ResponseEntity<Void> recuperarSenha(@Valid @RequestBody RecuperarSenhaRequest req) {
        recuperacaoSenhaService.solicitar(req.email());
        return ResponseEntity.noContent().build();
    }

    /** Troca a senha com o token do link (BP1-40). Publico. */
    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest req) {
        if (req.confirmacaoSenha() != null && !req.confirmacaoSenha().equals(req.novaSenha())) {
            throw new DadosInvalidosException("confirmacaoSenha", "Digite a mesma senha nos dois campos.");
        }
        recuperacaoSenhaService.redefinir(req.token(), req.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
