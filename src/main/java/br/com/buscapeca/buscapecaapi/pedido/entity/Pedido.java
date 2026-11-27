package br.com.buscapeca.buscapecaapi.pedido.entity;

import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pedido de uma loja dentro de uma compra (V7, V15). Nunca e apagado. */
@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    @Column(name = "id_compra", nullable = false, updatable = false)
    private Long idCompra;

    /** Loja do modulo loja. Guardado so o id (ADR-003). */
    @Column(name = "id_loja", nullable = false, updatable = false)
    private Long idLoja;

    @Column(name = "valor_itens", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorItens;

    /** Zero no MVP: o frete e combinado com a loja (ADR-012). */
    @Column(name = "valor_frete", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorFrete = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade_entrega", nullable = false, length = 20)
    private ModalidadeEntrega modalidadeEntrega;

    /** Recado do comprador para a loja no checkout (V15). */
    @Column(name = "observacao", length = 500)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusPedido status = StatusPedido.AGUARDANDO_ACEITE;

    @Column(name = "motivo_recusa", length = 255)
    private String motivoRecusa;

    @Column(name = "motivo_cancelamento", length = 255)
    private String motivoCancelamento;

    /** Conferido no balcao (BP1-29). So o comprador ve. Unico (uk_pedido_codigo_retirada). */
    @Column(name = "codigo_retirada", nullable = false, length = 8, updatable = false)
    private String codigoRetirada;

    @Column(name = "data_pedido", nullable = false, updatable = false)
    private LocalDateTime dataPedido;

    /** Preenchida pelo MySQL (ON UPDATE CURRENT_TIMESTAMP) e lida de volta depois de salvar. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "atualizado_em", insertable = false, updatable = false)
    private LocalDateTime atualizadoEm;

    /** Codigo que as telas mostram (ex.: BP-2081). */
    public String codigo() {
        return "BP-" + id;
    }
}
