package br.com.buscapeca.buscapecaapi.usuario.service;

import br.com.buscapeca.buscapecaapi.administracao.service.AuditoriaService;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.config.AppProperties;
import br.com.buscapeca.buscapecaapi.integracao.email.EmailService;
import br.com.buscapeca.buscapecaapi.usuario.entity.TokenVerificacao;
import br.com.buscapeca.buscapecaapi.usuario.entity.Usuario;
import br.com.buscapeca.buscapecaapi.usuario.enums.StatusUsuario;
import br.com.buscapeca.buscapecaapi.usuario.enums.TipoToken;
import br.com.buscapeca.buscapecaapi.usuario.repository.TokenVerificacaoRepository;
import br.com.buscapeca.buscapecaapi.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/**
 * Recuperacao de senha (BP1-40): link de uso unico, valido por 30 minutos,
 * com apenas o hash do token gravado no banco (ADR-006).
 */
@Service
public class RecuperacaoSenhaService {

    private static final int VALIDADE_MINUTOS = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacaoRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;
    private final AppProperties props;

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository, TokenVerificacaoRepository tokenRepository,
                                   PasswordEncoder passwordEncoder, EmailService emailService,
                                   AuditoriaService auditoriaService, AppProperties props) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditoriaService = auditoriaService;
        this.props = props;
    }

    /**
     * E-mail sem conta ativa devolve 404, como pede o fluxo alternativo do BP1-40
     * e a tela recuperar_senha.js ("Nao encontramos uma conta com este e-mail").
     */
    @Transactional
    public void solicitar(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(UsuarioService.normalizarEmail(email))
                .filter(u -> u.getStatus() == StatusUsuario.ATIVO)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Não encontramos uma conta com este e-mail."));
        tokenRepository.invalidarAnteriores(usuario.getId(), TipoToken.RECUPERAR_SENHA);

        String tokenPuro = gerarToken();
        TokenVerificacao token = new TokenVerificacao();
        token.setUsuario(usuario);
        token.setTipo(TipoToken.RECUPERAR_SENHA);
        token.setTokenHash(hash(tokenPuro));
        token.setDataExpiracao(LocalDateTime.now().plusMinutes(VALIDADE_MINUTOS));
        tokenRepository.save(token);

        String link = props.frontOrigem() + "/pages/auth/redefinir-senha.html?token=" + tokenPuro;
        emailService.enviar(usuario.getEmail(), "Busca Peça: redefinição de senha", """
                Olá, %s.

                Recebemos um pedido para redefinir a sua senha. Use o link abaixo em até %d minutos:
                %s

                Se não foi você, ignore este e-mail: a sua senha continua a mesma.
                """.formatted(usuario.getNome(), VALIDADE_MINUTOS, link));
        auditoriaService.registrar(usuario.getId(), "RECUPERAR_SENHA", "usuario", Map.of());
    }

    @Transactional
    public void redefinir(String tokenPuro, String novaSenha) {
        TokenVerificacao token = tokenRepository.findByTokenHashAndTipo(hash(tokenPuro), TipoToken.RECUPERAR_SENHA)
                .filter(t -> t.valido(LocalDateTime.now()))
                .orElseThrow(() -> new DadosInvalidosException("token",
                        "Este link é inválido ou expirou. Peça um novo link de recuperação."));
        token.setUsado(true);
        Usuario usuario = token.getUsuario();
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        auditoriaService.registrar(usuario.getId(), "REDEFINIR_SENHA", "usuario", Map.of());
    }

    private static String gerarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
