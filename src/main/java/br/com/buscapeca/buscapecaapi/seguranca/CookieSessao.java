package br.com.buscapeca.buscapecaapi.seguranca;

import br.com.buscapeca.buscapecaapi.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cookie que guarda o JWT: HttpOnly (o front nunca le o token), SameSite=Lax
 * e Secure em producao (ADR-006). Dominio em COOKIE_DOMINIO (ADR-015).
 */
@Component
public class CookieSessao {

    public static final String NOME = "BP_SESSAO";

    private final AppProperties props;

    public CookieSessao(AppProperties props) {
        this.props = props;
    }

    /** Sem "manter conectado" o cookie e de sessao: some ao fechar o navegador. */
    public ResponseCookie criar(String token, Duration validade, boolean manterConectado) {
        ResponseCookie.ResponseCookieBuilder cookie = base(token);
        if (manterConectado) {
            cookie.maxAge(validade);
        }
        return cookie.build();
    }

    public ResponseCookie apagar() {
        return base("").maxAge(0).build();
    }

    public Optional<String> ler(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> NOME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank())
                .map(Cookie::getValue)
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(NOME, valor)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Lax")
                .path("/");
        if (props.cookieDominio() != null && !props.cookieDominio().isBlank()) {
            cookie.domain(props.cookieDominio());
        }
        return cookie;
    }
}
