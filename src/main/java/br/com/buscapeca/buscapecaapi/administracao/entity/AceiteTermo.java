package br.com.buscapeca.buscapecaapi.administracao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Registro do aceite exigido pela LGPD (ADR-013). */
@Entity
@Table(name = "aceite_termo")
@Getter
@Setter
@NoArgsConstructor
public class AceiteTermo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aceite")
    private Long id;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_termo", nullable = false)
    private TermoUso termo;

    @Column(name = "data_aceite", nullable = false)
    private LocalDateTime dataAceite;
}
