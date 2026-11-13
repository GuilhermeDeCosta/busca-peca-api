package br.com.buscapeca.buscapecaapi.usuario.service;

import br.com.buscapeca.buscapecaapi.administracao.entity.Permissao;
import br.com.buscapeca.buscapecaapi.administracao.service.AuditoriaService;
import br.com.buscapeca.buscapecaapi.administracao.service.TermoUsoService;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.ConflitoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.NaoAutenticadoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.compartilhado.util.Documentos;
import br.com.buscapeca.buscapecaapi.usuario.dto.AtualizarPerfilRequest;
import br.com.buscapeca.buscapecaapi.usuario.dto.AutorizacaoUsuario;
import br.com.buscapeca.buscapecaapi.usuario.dto.CadastroUsuarioRequest;
import br.com.buscapeca.buscapecaapi.usuario.dto.TrocarSenhaRequest;
import br.com.buscapeca.buscapecaapi.usuario.dto.UsuarioResponse;
import br.com.buscapeca.buscapecaapi.usuario.entity.Papel;
import br.com.buscapeca.buscapecaapi.usuario.entity.Usuario;
import br.com.buscapeca.buscapecaapi.usuario.enums.NomePapel;
import br.com.buscapeca.buscapecaapi.usuario.enums.StatusUsuario;
import br.com.buscapeca.buscapecaapi.usuario.enums.TipoPessoa;
import br.com.buscapeca.buscapecaapi.usuario.mapper.UsuarioMapper;
import br.com.buscapeca.buscapecaapi.usuario.repository.PapelRepository;
import br.com.buscapeca.buscapecaapi.usuario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regras da conta de usuario (BP1-2, BP1-41, BP1-60). Outros modulos
 * usam este servico, nunca o UsuarioRepository (ADR-003).
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PapelRepository papelRepository;
    private final PasswordEncoder passwordEncoder;
    private final TermoUsoService termoUsoService;
    private final AuditoriaService auditoriaService;
    private final UsuarioMapper mapper;

    public UsuarioService(UsuarioRepository usuarioRepository, PapelRepository papelRepository,
                          PasswordEncoder passwordEncoder, TermoUsoService termoUsoService,
                          AuditoriaService auditoriaService, UsuarioMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.papelRepository = papelRepository;
        this.passwordEncoder = passwordEncoder;
        this.termoUsoService = termoUsoService;
        this.auditoriaService = auditoriaService;
        this.mapper = mapper;
    }

    /** Cadastro publico: toda conta nasce com o papel COMPRADOR (ADR-007). */
    @Transactional
    public UsuarioResponse cadastrar(CadastroUsuarioRequest req) {
        String email = normalizarEmail(req.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Este e-mail já está cadastrado.");
        }

        String documento = vazioParaNulo(Documentos.somenteDigitos(req.cnpjCpf()));
        TipoPessoa tipoPessoa = req.tipoPessoa() != null ? req.tipoPessoa()
                : documento != null && documento.length() == 14 ? TipoPessoa.PJ : TipoPessoa.PF;
        if (documento != null) {
            validarDocumento(tipoPessoa, documento);
            if (usuarioRepository.existsByCnpjCpf(documento)) {
                throw new ConflitoException("Este CPF/CNPJ já está cadastrado.");
            }
        }

        Usuario usuario = new Usuario();
        usuario.setNome(req.nome().trim());
        usuario.setNomeExibicao(vazioParaNulo(req.nomeExibicao()));
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(req.senha()));
        usuario.setTipoPessoa(tipoPessoa);
        usuario.setCnpjCpf(documento);
        usuario.setTelefone(vazioParaNulo(req.telefone()));
        usuario.setStatus(StatusUsuario.ATIVO);
        usuario.getPapeis().add(buscarPapel(NomePapel.COMPRADOR));
        usuario = usuarioRepository.save(usuario);

        termoUsoService.registrarAceiteDosVigentes(usuario.getId());
        auditoriaService.registrar(usuario.getId(), "CADASTRO", "usuario", Map.of("tipoPessoa", tipoPessoa.name()));
        return mapper.paraResponse(usuario);
    }

    /**
     * Confere e-mail e senha (BCrypt). A mensagem e a mesma para e-mail
     * inexistente e senha errada, para nao revelar quais e-mails existem.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse autenticar(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(email))
                .filter(u -> passwordEncoder.matches(senha, u.getSenhaHash()))
                .orElse(null);
        if (usuario == null) {
            auditoriaService.registrarSeparado(null, "LOGIN_FALHO", "usuario", Map.of("motivo", "CREDENCIAIS_INVALIDAS"));
            throw new NaoAutenticadoException("E-mail ou senha incorretos.");
        }
        if (usuario.getStatus() != StatusUsuario.ATIVO) {
            auditoriaService.registrarSeparado(usuario.getId(), "LOGIN_FALHO", "usuario",
                    Map.of("motivo", "CONTA_" + usuario.getStatus()));
            throw new NaoAutenticadoException(usuario.getStatus() == StatusUsuario.BLOQUEADO
                    ? "Esta conta está bloqueada. Fale com o suporte."
                    : "Esta conta está inativa.");
        }
        auditoriaService.registrarSeparado(usuario.getId(), "LOGIN", "usuario", Map.of());
        return mapper.paraResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPerfil(Long idUsuario) {
        return mapper.paraResponse(buscarComPapeis(idUsuario));
    }

    /** Campo ausente (null) fica como esta; texto vazio apaga o valor. */
    @Transactional
    public UsuarioResponse atualizarPerfil(Long idUsuario, AtualizarPerfilRequest req) {
        Usuario usuario = buscarComPapeis(idUsuario);
        if (req.nomeExibicao() != null) {
            usuario.setNomeExibicao(vazioParaNulo(req.nomeExibicao()));
        }
        if (req.telefone() != null) {
            usuario.setTelefone(vazioParaNulo(req.telefone()));
        }
        if (req.descricao() != null) {
            usuario.setDescricao(vazioParaNulo(req.descricao()));
        }
        return mapper.paraResponse(usuario);
    }

    @Transactional
    public void trocarSenha(Long idUsuario, TrocarSenhaRequest req) {
        Usuario usuario = buscarComPapeis(idUsuario);
        if (!passwordEncoder.matches(req.senhaAtual(), usuario.getSenhaHash())) {
            throw new DadosInvalidosException("senhaAtual", "A senha atual está incorreta.");
        }
        usuario.setSenhaHash(passwordEncoder.encode(req.novaSenha()));
        auditoriaService.registrar(idUsuario, "ALTERAR_SENHA", "usuario", Map.of());
    }

    /** Usado pela seguranca a cada requisicao autenticada. */
    @Transactional(readOnly = true)
    public AutorizacaoUsuario carregarAutorizacao(Long idUsuario) {
        Usuario usuario = usuarioRepository.findComPapeisById(idUsuario)
                .orElseThrow(() -> new NaoAutenticadoException("Sessão inválida. Entre novamente."));
        Set<String> papeis = usuario.getPapeis().stream().map(Papel::getNome).collect(Collectors.toSet());
        Set<String> permissoes = usuario.getPapeis().stream()
                .flatMap(p -> p.getPermissoes().stream())
                .map(Permissao::getCodigo)
                .collect(Collectors.toSet());
        return new AutorizacaoUsuario(usuario.getId(), usuario.getStatus(), papeis, permissoes);
    }

    // ---- uso interno do modulo usuario ----

    Usuario buscarComPapeis(Long idUsuario) {
        return usuarioRepository.findComPapeisById(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private Papel buscarPapel(NomePapel nome) {
        return papelRepository.findByNome(nome.name())
                .orElseThrow(() -> new IllegalStateException("Papel " + nome + " ausente; confira a migracao V9."));
    }

    private void validarDocumento(TipoPessoa tipo, String documento) {
        if (tipo == TipoPessoa.PF && !Documentos.cpfValido(documento)) {
            throw new DadosInvalidosException("cnpjCpf", "CPF inválido.");
        }
        if (tipo == TipoPessoa.PJ && !Documentos.cnpjValido(documento)) {
            throw new DadosInvalidosException("cnpjCpf", "CNPJ inválido.");
        }
    }

    static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
