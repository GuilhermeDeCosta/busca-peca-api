package br.com.buscapeca.buscapecaapi.pedido.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Peca de um pedido, com o preco do momento da compra (V7). */
@Entity
@Table(name = "item_pedido")
@Getter
@Setter
@NoArgsConstructor
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item_pedido")
    private Long id;

    @Column(name = "id_pedido", nullable = false, updatable = false)
    private Long idPedido;

    @Column(name = "id_peca", nullable = false, updatable = false)
    private Long idPeca;

    @Column(name = "quantidade", nullable = false, updatable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal precoUnitario;

    public ItemPedido(Long idPedido, Long idPeca, int quantidade, BigDecimal precoUnitario) {
        this.idPedido = idPedido;
        this.idPeca = idPeca;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }
}
