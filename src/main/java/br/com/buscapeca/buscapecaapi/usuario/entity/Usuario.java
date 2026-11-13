package br.com.buscapeca.buscapecaapi.usuario.entity;

import br.com.buscapeca.buscapecaapi.usuario.enums.NomePapel;
import br.com.buscapeca.buscapecaapi.usuario.enums.StatusUsuario;
import br.com.buscapeca.buscapecaapi.usuario.enums.TipoPessoa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Conta unica, que pode ser compradora e vendedora ao mesmo tempo (ADR-007).
 * Nunca e apagada: a exclusao e logica (status INATIVO) com anonimizacao (ADR-013).
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "nome_exibicao", length = 80)
    private String nomeExibicao;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 2)
    private TipoPessoa tipoPessoa;

    @Column(name = "cnpj_cpf", length = 14)
    private String cnpjCpf;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "identidade_verificada", nullable = false)
    private boolean identidadeVerificada;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusUsuario status = StatusUsuario.ATIVO;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    /** Preenchida pelo proprio MySQL (ON UPDATE CURRENT_TIMESTAMP). */
    @Column(name = "atualizado_em", insertable = false, updatable = false)
    private LocalDateTime atualizadoEm;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_papel",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_papel"))
    private Set<Papel> papeis = new HashSet<>();

    @PrePersist
    void aoCriar() {
        if (dataCadastro == null) {
            dataCadastro = LocalDateTime.now();
        }
    }

    public boolean temPapel(NomePapel papel) {
        return papeis.stream().anyMatch(p -> p.getNome().equals(papel.name()));
    }
}
