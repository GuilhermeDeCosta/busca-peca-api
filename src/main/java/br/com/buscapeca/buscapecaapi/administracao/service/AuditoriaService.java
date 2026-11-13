package br.com.buscapeca.buscapecaapi.administracao.service;

import br.com.buscapeca.buscapecaapi.administracao.entity.LogAuditoria;
import br.com.buscapeca.buscapecaapi.administracao.repository.LogAuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Grava acoes sensiveis em log_auditoria (ADR-013).
 * Nunca passe dados pessoais (e-mail, CPF, nome) nos parametros.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final LogAuditoriaRepository repository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transacaoPropria;

    public AuditoriaService(LogAuditoriaRepository repository, ObjectMapper objectMapper,
                            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.transacaoPropria = new TransactionTemplate(transactionManager);
        this.transacaoPropria.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Grava dentro da transacao de quem chamou: se a operacao for desfeita,
     * o registro tambem e (ex.: cadastro, troca de senha).
     */
    @Transactional
    public void registrar(Long idUsuario, String acao, String entidade, Map<String, Object> parametros) {
        repository.save(montar(idUsuario, acao, entidade, parametros));
    }

    /**
     * Grava numa transacao separada, que fica salva mesmo se a operacao principal
     * falhar (ex.: tentativa de login com senha errada). Uma falha aqui so vai
     * para o log e nunca interrompe a operacao do usuario.
     */
    public void registrarSeparado(Long idUsuario, String acao, String entidade, Map<String, Object> parametros) {
        try {
            transacaoPropria.executeWithoutResult(s -> repository.saveAndFlush(
                    montar(idUsuario, acao, entidade, parametros)));
        } catch (RuntimeException e) {
            log.error("Falha ao gravar auditoria {} / {}: {}", acao, entidade, e.getMessage());
        }
    }

    private LogAuditoria montar(Long idUsuario, String acao, String entidade, Map<String, Object> parametros) {
        LogAuditoria registro = new LogAuditoria();
        registro.setIdUsuario(idUsuario);
        registro.setAcao(acao);
        registro.setEntidade(entidade);
        registro.setParametrosAnonimizados(
                parametros == null || parametros.isEmpty() ? null : objectMapper.writeValueAsString(parametros));
        registro.setDataRegistro(LocalDateTime.now());
        return registro;
    }
}
