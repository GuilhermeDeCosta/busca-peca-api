package br.com.buscapeca.buscapecaapi.seguranca.dto;

/**
 * Resposta do GET /auth/csrf. Mesmos nomes do CsrfToken do Spring
 * (o login.js le "headerName" e "token").
 */
public record CsrfResponse(String headerName, String parameterName, String token) {
}
