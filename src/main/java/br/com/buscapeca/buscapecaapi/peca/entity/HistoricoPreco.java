package br.com.buscapeca.buscapecaapi.peca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Uma mudanca de preco do anuncio (BP1-53). A primeira linha e a publicacao e tem o preco
 * anterior nulo (V13). Nunca e alterada depois de gravada, por isso nao tem setters.
 * Guarda so o id da peca, sem ligar com a entidade, como os outros registros do modulo.
 */
@Entity
@Table(name = "historico_preco")
@Getter
@NoArgsConstructor
public class HistoricoPreco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historico")
    private Long id;

    @Column(name = "id_peca", nullable = false, updatable = false)
    private Long idPeca;

    /** Nulo na linha da publicacao. */
    @Column(name = "preco_anterior", precision = 10, scale = 2, updatable = false)
    private BigDecimal precoAnterior;

    @Column(name = "preco_novo", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal precoNovo;

    @Column(name = "data_alteracao", nullable = false, updatable = false)
    private LocalDateTime dataAlteracao;

    public HistoricoPreco(Long idPeca, BigDecimal precoAnterior, BigDecimal precoNovo) {
        this.idPeca = idPeca;
        this.precoAnterior = precoAnterior;
        this.precoNovo = precoNovo;
    }

    @PrePersist
    void aoCriar() {
        if (dataAlteracao == null) {
            dataAlteracao = LocalDateTime.now();
        }
    }
}
