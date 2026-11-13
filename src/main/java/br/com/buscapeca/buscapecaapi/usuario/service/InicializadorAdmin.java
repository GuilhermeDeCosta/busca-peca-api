package br.com.buscapeca.buscapecaapi.usuario.service;

import br.com.buscapeca.buscapecaapi.config.AppProperties;
import br.com.buscapeca.buscapecaapi.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A migracao V9 cria o administrador inicial sem senha utilizavel.
 * Na subida, se ADMIN_SENHA estiver definida e o admin ainda nao tiver senha,
 * ela e gravada com BCrypt. Assim nenhuma senha fica no repositorio.
 */
@Component
public class InicializadorAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializadorAdmin.class);
    private static final String SEM_SENHA = "{bloqueada}";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public InicializadorAdmin(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                              AppProperties props) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = props.admin();
        if (admin == null || admin.email() == null) {
            return;
        }
        usuarioRepository.findByEmailIgnoreCase(admin.email())
                .filter(u -> SEM_SENHA.equals(u.getSenhaHash()))
                .ifPresent(u -> {
                    if (admin.senha() == null || admin.senha().isBlank()) {
                        log.warn("Administrador inicial {} sem senha: defina ADMIN_SENHA e reinicie.", admin.email());
                        return;
                    }
                    u.setSenhaHash(passwordEncoder.encode(admin.senha()));
                    log.info("Senha do administrador inicial definida a partir de ADMIN_SENHA.");
                });
    }
}
