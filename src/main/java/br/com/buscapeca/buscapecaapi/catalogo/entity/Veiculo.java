package br.com.buscapeca.buscapecaapi.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "veiculo")
@Getter
@NoArgsConstructor
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_veiculo")
    private Long id;

    @Column(name = "marca", nullable = false, length = 50)
    private String marca;

    @Column(name = "modelo", nullable = false, length = 80)
    private String modelo;

    @Column(name = "ano", nullable = false)
    private Short ano;

    @Column(name = "versao", length = 80)
    private String versao;

    public Veiculo(String marca, String modelo, Short ano, String versao) {
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.versao = versao;
    }
}
