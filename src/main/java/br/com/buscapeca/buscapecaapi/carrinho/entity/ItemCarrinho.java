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

/**
 * Peca no carrinho (V7). Uma linha por peca (uk_item_carrinho_peca): adicionar de novo
 * soma na quantidade. O preco nao fica gravado: o carrinho sempre mostra o preco de agora.
 */
@Entity
@Table(name = "item_carrinho")
@Getter
@Setter
@NoArgsConstructor
public class ItemCarrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item_carrinho")
    private Long id;

    @Column(name = "id_carrinho", nullable = false, updatable = false)
    private Long idCarrinho;

    /** Peca do modulo peca. Guardado so o id (ADR-003). */
    @Column(name = "id_peca", nullable = false, updatable = false)
    private Long idPeca;

    @Column(name = "quantidade", nullable = false)
    private int quantidade;

    public ItemCarrinho(Long idCarrinho, Long idPeca, int quantidade) {
        this.idCarrinho = idCarrinho;
        this.idPeca = idPeca;
        this.quantidade = quantidade;
    }
}
