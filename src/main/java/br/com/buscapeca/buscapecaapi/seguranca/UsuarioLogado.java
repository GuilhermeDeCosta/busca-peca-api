package br.com.buscapeca.buscapecaapi.seguranca;

import br.com.buscapeca.buscapecaapi.compartilhado.excecao.NaoAutenticadoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Quem esta fazendo a requisicao. Use nos servicos para conferir o dono
 * de um registro (Documentacao do Back-end, secao 6).
 */
@Component
public class UsuarioLogado {

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return Long.valueOf(jwt.getToken().getSubject());
        }
        throw new NaoAutenticadoException("Faça login para continuar.");
    }

    public boolean temPapel(String papel) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + papel));
    }
}
