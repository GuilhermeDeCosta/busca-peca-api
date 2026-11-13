package br.com.buscapeca.buscapecaapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracoes proprias do Busca Peca (prefixo "app" nos application*.yml).
 * Os valores reais de producao vem de variaveis de ambiente (ADR-015).
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontOrigem,
        boolean cookieSecure,
        String cookieDominio,
        String uploadDiretorio,
        Jwt jwt,
        Admin admin
) {

    public record Jwt(String segredo, long expiracaoMinutos) {
    }

    public record Admin(String email, String senha) {
    }
}
