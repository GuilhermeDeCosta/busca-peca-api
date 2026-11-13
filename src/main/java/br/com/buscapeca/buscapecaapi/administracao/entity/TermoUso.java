package br.com.buscapeca.buscapecaapi.administracao.entity;

import br.com.buscapeca.buscapecaapi.administracao.enums.TipoTermo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "termo_uso")
@Getter
@Setter
@NoArgsConstructor
public class TermoUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_termo")
    private Integer id;

    @Column(name = "id_admin_publicador", nullable = false)
    private Long idAdminPublicador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoTermo tipo;

    @Column(name = "versao", nullable = false, length = 10)
    private String versao;

    @Column(name = "conteudo", nullable = false, columnDefinition = "mediumtext")
    private String conteudo;

    @Column(name = "data_publicacao", nullable = false)
    private LocalDateTime dataPublicacao;

    @Column(name = "vigente", nullable = false)
    private boolean vigente;
}
