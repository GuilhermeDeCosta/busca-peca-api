package br.com.buscapeca.buscapecaapi.administracao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Acoes sensiveis, com parametros sem dados pessoais (ADR-013). Nunca e apagado. */
@Entity
@Table(name = "log_auditoria")
@Getter
@Setter
@NoArgsConstructor
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long id;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "acao", nullable = false, length = 50)
    private String acao;

    @Column(name = "entidade", nullable = false, length = 50)
    private String entidade;

    /** JSON gravado como texto pelo AuditoriaService. */
    @Column(name = "parametros_anonimizados", columnDefinition = "json")
    private String parametrosAnonimizados;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;
}
