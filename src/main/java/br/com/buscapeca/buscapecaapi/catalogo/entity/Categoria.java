package br.com.buscapeca.buscapecaapi.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Categoria hierarquica: id_categoria_pai nulo nas principais (ex.: Freios -> Pastilhas). */
@Entity
@Table(name = "categoria")
@Getter
@NoArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer id;

    @Column(name = "id_categoria_pai")
    private Integer idCategoriaPai;

    @Column(name = "nome", nullable = false, length = 80)
    private String nome;
}
