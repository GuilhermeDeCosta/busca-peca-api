package br.com.buscapeca.buscapecaapi.integracao.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio real por SMTP no perfil prod (EMAIL_HOST, EMAIL_USUARIO, EMAIL_SENHA).
 * Timeout de 10 s configurado em application-prod.yml.
 */
@Service
@Profile("prod")
public class EmailServiceSmtp implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceSmtp.class);

    private final JavaMailSender mailSender;

    public EmailServiceSmtp(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public boolean enviar(String destinatario, String assunto, String texto) {
        try {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setTo(destinatario);
            mensagem.setSubject(assunto);
            mensagem.setText(texto);
            mailSender.send(mensagem);
            return true;
        } catch (MailException e) {
            // Alternativa em caso de falha (ADR-012): registra e deixa reenviar.
            log.error("Falha ao enviar e-mail '{}': {}", assunto, e.getMessage());
            return false;
        }
    }
}
