package br.com.buscapeca.buscapecaapi.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Le o JWT do cookie HttpOnly BP_SESSAO, valida e autentica a requisicao (ADR-006).
 *
 * Por que um filtro proprio e nao o oauth2ResourceServer do Spring: o resource server
 * desliga o CSRF em toda requisicao que traz token, o que e certo para o cabecalho
 * Authorization, mas abriria brecha de CSRF com o token em cookie. Aqui o CSRF
 * continua valendo em POST, PUT, PATCH e DELETE.
 *
 * Nas rotas publicas o cookie e ignorado, para uma sessao vencida nao bloquear
 * paginas abertas (ex.: lista de categorias). Excecao: as rotas de
 * RotasPublicas.SESSAO_OPCIONAL leem o cookie, mas seguem sem login se ele for invalido.
 */
public class CookieJwtFilter extends OncePerRequestFilter {

    private final CookieSessao cookieSessao;
    private final JwtDecoder jwtDecoder;
    private final ConversorAutenticacao conversor;
    private final AuthenticationEntryPoint entryPoint;

    public CookieJwtFilter(CookieSessao cookieSessao, JwtDecoder jwtDecoder, ConversorAutenticacao conversor,
                           AuthenticationEntryPoint entryPoint) {
        this.cookieSessao = cookieSessao;
        this.jwtDecoder = jwtDecoder;
        this.conversor = conversor;
        this.entryPoint = entryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean sessaoOpcional = RotasPublicas.SESSAO_OPCIONAL.matches(request);
        Optional<String> token = RotasPublicas.TODAS.matches(request) && !sessaoOpcional
                ? Optional.empty()
                : cookieSessao.ler(request);
        if (token.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Jwt jwt = jwtDecoder.decode(token.get());
            AbstractAuthenticationToken autenticacao = conversor.convert(jwt);
            autenticacao.setDetails(request.getRemoteAddr());
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacao);
            SecurityContextHolder.setContext(contexto);
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
            if (sessaoOpcional) {
                chain.doFilter(request, response);
                return;
            }
            entryPoint.commence(request, response, new InvalidBearerTokenException("Token inválido ou vencido.", e));
            return;
        } catch (AuthenticationException e) {
            SecurityContextHolder.clearContext();
            if (sessaoOpcional) {
                chain.doFilter(request, response);
                return;
            }
            entryPoint.commence(request, response, e);
            return;
        }
        chain.doFilter(request, response);
    }
}
