package br.com.buscapeca.buscapecaapi.integracao.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Implementacao falsa do perfil dev (ADR-012): em vez de enviar,
 * escreve o e-mail no console. Util para pegar o link de redefinir senha.
 */
@Service
@Profile("!prod")
public class EmailServiceLog implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceLog.class);

    @Override
    public boolean enviar(String destinatario, String assunto, String texto) {
        log.info("""

                ===== E-MAIL (perfil dev, nao enviado) =====
                Para: {}
                Assunto: {}
                {}
                ============================================""", destinatario, assunto, texto);
        return true;
    }
}
