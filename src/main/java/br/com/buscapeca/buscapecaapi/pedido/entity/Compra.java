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
import java.time.LocalDateTime;

/**
 * Uma finalizacao de compra (V7). Gera um Pedido por loja (ADR-009). Nenhum valor e
 * cobrado na plataforma: o pagamento e combinado com cada loja (ADR-008).
 */
@Entity
@Table(name = "compra")
@Getter
@Setter
@NoArgsConstructor
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    private Long id;

    @Column(name = "id_comprador", nullable = false, updatable = false)
    private Long idComprador;

    /** Preenchido so quando algum pedido da compra e ENTREGA. */
    @Column(name = "id_endereco_entrega")
    private Long idEnderecoEntrega;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "data_compra", nullable = false)
    private LocalDateTime dataCompra;
}
