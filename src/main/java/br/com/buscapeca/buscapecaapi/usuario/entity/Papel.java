package br.com.buscapeca.buscapecaapi.usuario.entity;

import br.com.buscapeca.buscapecaapi.administracao.entity.Permissao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "papel")
@Getter
@NoArgsConstructor
public class Papel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_papel")
    private Integer id;

    @Column(name = "nome", nullable = false, length = 30)
    private String nome;

    @Column(name = "descricao", length = 200)
    private String descricao;

    /** Papeis internos (ADMINISTRADOR) nunca sao escolhidos no cadastro publico. */
    @Column(name = "interno", nullable = false)
    private boolean interno;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "papel_permissao",
            joinColumns = @JoinColumn(name = "id_papel"),
            inverseJoinColumns = @JoinColumn(name = "id_permissao"))
    private Set<Permissao> permissoes = new HashSet<>();
}
