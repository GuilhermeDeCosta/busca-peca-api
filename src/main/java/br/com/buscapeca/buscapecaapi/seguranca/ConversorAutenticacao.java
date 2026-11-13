package br.com.buscapeca.buscapecaapi.seguranca;

import br.com.buscapeca.buscapecaapi.compartilhado.excecao.NaoAutenticadoException;
import br.com.buscapeca.buscapecaapi.usuario.dto.AutorizacaoUsuario;
import br.com.buscapeca.buscapecaapi.usuario.enums.StatusUsuario;
import br.com.buscapeca.buscapecaapi.usuario.service.UsuarioService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Transforma o JWT valido em usuario autenticado. Papeis e permissoes sao lidos
 * do banco a cada requisicao: conta suspensa ou papel removido valem na hora,
 * sem esperar o token vencer (ADR-006, "Consequencias negativas").
 *
 * Autoridades geradas: ROLE_COMPRADOR, ROLE_VENDEDOR, ROLE_ADMINISTRADOR e as
 * permissoes pelo codigo (ex.: GERENCIAR_USUARIOS), para usar em @PreAuthorize.
 */
public class ConversorAutenticacao implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioService usuarioService;

    public ConversorAutenticacao(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AutorizacaoUsuario autorizacao;
        try {
            autorizacao = usuarioService.carregarAutorizacao(Long.valueOf(jwt.getSubject()));
        } catch (NaoAutenticadoException | NumberFormatException e) {
            throw new InvalidBearerTokenException("Sessão inválida.");
        }
        if (autorizacao.status() != StatusUsuario.ATIVO) {
            throw new InvalidBearerTokenException("Conta " + autorizacao.status());
        }
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autorizacao.papeis().forEach(p -> autoridades.add(new SimpleGrantedAuthority("ROLE_" + p)));
        autorizacao.permissoes().forEach(p -> autoridades.add(new SimpleGrantedAuthority(p)));
        return new JwtAuthenticationToken(jwt, autoridades, jwt.getSubject());
    }
}
