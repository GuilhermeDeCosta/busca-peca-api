package br.com.buscapeca.buscapecaapi.seguranca;

import br.com.buscapeca.buscapecaapi.administracao.service.TermoUsoService;
import br.com.buscapeca.buscapecaapi.compartilhado.erro.ErroResposta;
import br.com.buscapeca.buscapecaapi.seguranca.dto.SessaoResponse;
import br.com.buscapeca.buscapecaapi.usuario.dto.UsuarioResponse;
import br.com.buscapeca.buscapecaapi.usuario.enums.NomePapel;
import br.com.buscapeca.buscapecaapi.usuario.service.UsuarioService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;

/** Login e "quem sou eu" (BP1-41). A conferencia de senha fica no UsuarioService. */
@Service
public class AuthService {

    private final UsuarioService usuarioService;
    private final TermoUsoService termoUsoService;
    private final JwtService jwtService;

    public AuthService(UsuarioService usuarioService, TermoUsoService termoUsoService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.termoUsoService = termoUsoService;
        this.jwtService = jwtService;
    }

    public Login entrar(String email, String senha) {
        UsuarioResponse usuario = usuarioService.autenticar(email, senha);
        JwtService.TokenEmitido token = jwtService.emitir(usuario.idUsuario(), usuario.papeis());
        return new Login(token, sessao(usuario, token.expiraEm()));
    }

    public SessaoResponse sessaoAtual(Long idUsuario, Instant expiraEm) {
        return sessao(usuarioService.buscarPerfil(idUsuario), expiraEm);
    }

    private SessaoResponse sessao(UsuarioResponse u, Instant expiraEm) {
        return new SessaoResponse(
                u.idUsuario(),
                u.nome(),
                u.nomeExibicao(),
                u.email(),
                u.papeis(),
                u.papeis().contains(NomePapel.VENDEDOR.name()),
                u.papeis().contains(NomePapel.ADMINISTRADOR.name()),
                termoUsoService.temAceitePendente(u.idUsuario()),
                expiraEm == null ? null : OffsetDateTime.ofInstant(expiraEm, ErroResposta.FUSO));
    }

    public record Login(JwtService.TokenEmitido token, SessaoResponse sessao) {
    }
}
