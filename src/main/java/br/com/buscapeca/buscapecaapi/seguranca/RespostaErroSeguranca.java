package br.com.buscapeca.buscapecaapi.seguranca;

import br.com.buscapeca.buscapecaapi.compartilhado.erro.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respostas 401 e 403 geradas pela seguranca, no mesmo formato de erro da API.
 * Quando a sessao e invalida, o cookie e apagado junto.
 */
public class RespostaErroSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final CookieSessao cookieSessao;

    public RespostaErroSeguranca(ObjectMapper objectMapper, CookieSessao cookieSessao) {
        this.objectMapper = objectMapper;
        this.cookieSessao = cookieSessao;
    }

    /** 401: sem login ou token vencido/invalido. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        boolean tinhaSessao = cookieSessao.ler(request).isPresent();
        if (tinhaSessao) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookieSessao.apagar().toString());
        }
        String mensagem = tinhaSessao
                ? "Sua sessão expirou. Entre novamente."
                : "Faça login para continuar.";
        escrever(request, response, HttpServletResponse.SC_UNAUTHORIZED, "NAO_AUTENTICADO", mensagem);
    }

    /** 403: logado sem permissao, ou token CSRF ausente/invalido. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        if (ex instanceof CsrfException) {
            escrever(request, response, HttpServletResponse.SC_FORBIDDEN, "CSRF_INVALIDO",
                    "Token de segurança ausente ou vencido. Recarregue a página e tente de novo.");
            return;
        }
        escrever(request, response, HttpServletResponse.SC_FORBIDDEN, "ACESSO_NEGADO",
                "Você não tem permissão para esta ação.");
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response, int status,
                          String codigo, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                ErroResposta.de(status, codigo, mensagem, request.getRequestURI()));
    }
}
