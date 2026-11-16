package br.com.buscapeca.buscapecaapi.peca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Veiculo em que a peca serve: uma linha por ano (BP1-45). O veiculo fica no modulo catalogo. */
@Entity
@Table(name = "compatibilidade")
@Getter
@NoArgsConstructor
public class Compatibilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compatibilidade")
    private Long id;

    @Column(name = "id_peca", nullable = false)
    private Long idPeca;

    @Column(name = "id_veiculo", nullable = false)
    private Long idVeiculo;

    public Compatibilidade(Long idPeca, Long idVeiculo) {
        this.idPeca = idPeca;
        this.idVeiculo = idVeiculo;
    }
}
