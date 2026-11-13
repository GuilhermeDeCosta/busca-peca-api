package br.com.buscapeca.buscapecaapi.seguranca;

import br.com.buscapeca.buscapecaapi.config.AppProperties;
import br.com.buscapeca.buscapecaapi.usuario.service.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Seguranca da API (ADR-006, Documentacao do Back-end secoes 2 e 6):
 * - sessao sem estado: JWT no cookie HttpOnly BP_SESSAO, lido pelo CookieJwtFilter;
 * - CSRF: o front busca o token em GET /api/auth/csrf e envia no cabecalho X-XSRF-TOKEN;
 * - CORS: so a origem do front (FRONT_ORIGEM), com credenciais;
 * - quem decide o acesso e a API; papeis e permissoes vem do banco a cada requisicao.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtService jwtService) {
        return jwtService.decoder();
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, AppProperties props, CookieSessao cookieSessao,
                                       UsuarioService usuarioService, ObjectMapper objectMapper,
                                       JwtDecoder jwtDecoder) throws Exception {
        RespostaErroSeguranca respostaErro = new RespostaErroSeguranca(objectMapper, cookieSessao);

        // Token CSRF em cookie legivel pelo JS (XSRF-TOKEN) e conferido no cabecalho X-XSRF-TOKEN.
        CookieCsrfTokenRepository repositorioCsrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repositorioCsrf.setCookieCustomizer(c -> {
            c.path("/").sameSite("Lax").secure(props.cookieSecure());
            if (props.cookieDominio() != null && !props.cookieDominio().isBlank()) {
                c.domain(props.cookieDominio());
            }
        });
        CsrfTokenRequestAttributeHandler leitorCsrf = new CsrfTokenRequestAttributeHandler();
        leitorCsrf.setCsrfRequestAttributeName(null); // gera o token ja na primeira requisicao

        http
                .cors(cors -> {})
                .csrf(csrf -> csrf
                        .csrfTokenRepository(repositorioCsrf)
                        .csrfTokenRequestHandler(leitorCsrf)
                        .ignoringRequestMatchers(RotasPublicas.SEM_CSRF))
                // Sem sessao no servidor: a identidade vem do JWT a cada requisicao. O SessionManagement
                // fica desligado porque, sem sessao, ele trataria toda requisicao como "novo login"
                // e trocaria o token CSRF a cada chamada.
                .sessionManagement(s -> s.disable())
                .securityContext(c -> c.securityContextRepository(new RequestAttributeSecurityContextRepository()))
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .logout(l -> l.disable())
                .requestCache(c -> c.disable())
                .authorizeHttpRequests(a -> a
                        .requestMatchers(RotasPublicas.TODAS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new CookieJwtFilter(cookieSessao, jwtDecoder,
                                new ConversorAutenticacao(usuarioService), respostaErro),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respostaErro)
                        .accessDeniedHandler(respostaErro));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(props.frontOrigem()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, "X-XSRF-TOKEN"));
        config.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", config);
        return fonte;
    }
}
