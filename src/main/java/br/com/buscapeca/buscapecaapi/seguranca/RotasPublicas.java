package br.com.buscapeca.buscapecaapi.seguranca;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Rotas abertas sem login (Documentacao do Back-end, secao 5, acesso "Publico").
 * Os caminhos nao incluem o /api, que e o context-path da aplicacao.
 * Ao criar uma rota publica nova, acrescente aqui.
 */
public final class RotasPublicas {

    private static final PathPatternRequestMatcher.Builder ROTA = PathPatternRequestMatcher.withDefaults();

    /**
     * Rotas sem login. As que mudam dados (POST) continuam exigindo CSRF,
     * exceto as listadas em SEM_CSRF. O logout fica aberto para sempre
     * conseguir apagar o cookie, mesmo com a sessao vencida.
     */
    public static final RequestMatcher TODAS = new OrRequestMatcher(
            ROTA.matcher(HttpMethod.POST, "/auth/login"),
            ROTA.matcher(HttpMethod.POST, "/auth/logout"),
            ROTA.matcher(HttpMethod.GET, "/auth/csrf"),
            ROTA.matcher(HttpMethod.POST, "/auth/recuperar-senha"),
            ROTA.matcher(HttpMethod.POST, "/auth/redefinir-senha"),
            ROTA.matcher(HttpMethod.POST, "/usuarios"),
            ROTA.matcher(HttpMethod.GET, "/termos/vigentes"),
            ROTA.matcher(HttpMethod.GET, "/categorias"),
            ROTA.matcher(HttpMethod.GET, "/categorias/**"),
            ROTA.matcher(HttpMethod.GET, "/veiculos"),
            ROTA.matcher(HttpMethod.GET, "/veiculos/**"),
            ROTA.matcher(HttpMethod.GET, "/tags"),
            // Loja publica: so o id numerico, para /lojas/minha continuar exigindo login
            ROTA.matcher(HttpMethod.GET, "/lojas/{id:\\d+}"),
            ROTA.matcher(HttpMethod.GET, "/actuator/health"),
            ROTA.matcher(HttpMethod.GET, "/actuator/health/**"),
            ROTA.matcher("/swagger-ui.html"),
            ROTA.matcher("/swagger-ui/**"),
            ROTA.matcher("/v3/api-docs/**"),
            ROTA.matcher("/error"));

    /**
     * Rotas sem verificacao de CSRF. Hoje so o cadastro: o cadastro.js do front
     * nao envia o token, e criar conta nao abre sessao nem altera dados de ninguem.
     */
    public static final RequestMatcher SEM_CSRF = new OrRequestMatcher(
            ROTA.matcher(HttpMethod.POST, "/usuarios"));

    private RotasPublicas() {
    }
}
