package br.com.buscapeca.buscapecaapi.pedido.entity;

import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEnvio;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusEntrega;
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

import java.time.LocalDateTime;

/** Envio de um pedido de ENTREGA (V7): uma por pedido (uk_entrega_pedido), criada em "enviar". */
@Entity
@Table(name = "entrega")
@Getter
@Setter
@NoArgsConstructor
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrega")
    private Long id;

    @Column(name = "id_pedido", nullable = false, updatable = false)
    private Long idPedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade", nullable = false, length = 20)
    private ModalidadeEnvio modalidade;

    @Column(name = "transportadora", length = 80)
    private String transportadora;

    @Column(name = "codigo_rastreio", length = 50)
    private String codigoRastreio;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_entrega", nullable = false, length = 20)
    private StatusEntrega statusEntrega;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;
}
