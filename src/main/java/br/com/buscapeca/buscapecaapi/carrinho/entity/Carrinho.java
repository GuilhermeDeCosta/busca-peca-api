package br.com.buscapeca.buscapecaapi.carrinho.entity;

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

/**
 * Carrinho do usuario (V7): um por conta (uk_carrinho_usuario), criado no primeiro item.
 * Os itens ficam em ItemCarrinho e podem ser de varias lojas (ADR-009).
 */
@Entity
@Table(name = "carrinho")
@Getter
@Setter
@NoArgsConstructor
public class Carrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrinho")
    private Long id;

    /** Dono do carrinho. Guardado so o id, como na Loja. */
    @Column(name = "id_usuario", nullable = false, updatable = false)
    private Long idUsuario;

    /** Preenchida pelo proprio MySQL (ON UPDATE CURRENT_TIMESTAMP). */
    @Column(name = "atualizado_em", insertable = false, updatable = false)
    private LocalDateTime atualizadoEm;

    public Carrinho(Long idUsuario) {
        this.idUsuario = idUsuario;
    }
}
