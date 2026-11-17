package br.com.buscapeca.buscapecaapi.peca.entity;

import br.com.buscapeca.buscapecaapi.peca.enums.CondicaoPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.OrigemPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;
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

/**
 * Anuncio de peca de uma loja (BP1-3, 45). Rascunho so exige o titulo; categoria,
 * codigo OEM e preco sao exigidos pela API na publicacao (V12).
 */
@Entity
@Table(name = "peca")
@Getter
@Setter
@NoArgsConstructor
public class Peca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_peca")
    private Long id;

    /** Loja que anuncia. Guardado so o id, como o idUsuario na Loja. */
    @Column(name = "id_loja", nullable = false, updatable = false)
    private Long idLoja;

    @Column(name = "id_categoria")
    private Integer idCategoria;

    /** Maiusculas, sem espacos, pontos ou hifens. */
    @Column(name = "codigo_oem", length = 50)
    private String codigoOem;

    @Column(name = "codigo_ean", length = 14)
    private String codigoEan;

    @Column(name = "codigo_sku", length = 50)
    private String codigoSku;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "text")
    private String descricao;

    @Column(name = "marca_peca", length = 60)
    private String marcaPeca;

    /** Caracteristicas tecnicas em uma linha (ex.: "12 V • 90 A"). */
    @Column(name = "caracteristicas", length = 255)
    private String caracteristicas;

    @Column(name = "preco", precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_estoque", nullable = false)
    private int quantidadeEstoque;

    /** Abaixo disso o vendedor recebe alerta (BP1-52). */
    @Column(name = "limite_alerta_estoque", nullable = false)
    private int limiteAlertaEstoque = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicao", nullable = false, length = 20)
    private CondicaoPeca condicao = CondicaoPeca.NOVA;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 20)
    private OrigemPeca origem = OrigemPeca.NACIONAL;

    @Column(name = "pronta_entrega", nullable = false)
    private boolean prontaEntrega = true;

    @Column(name = "prazo_expedicao_dias", nullable = false)
    private int prazoExpedicaoDias;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPeca status = StatusPeca.RASCUNHO;

    /** Preenchida pelo MySQL (DEFAULT CURRENT_TIMESTAMP) e lida de volta depois do insert. */
    @Generated(event = EventType.INSERT)
    @Column(name = "data_cadastro", insertable = false, updatable = false)
    private LocalDateTime dataCadastro;

    /** Preenchida pelo MySQL (ON UPDATE CURRENT_TIMESTAMP) e lida de volta depois de salvar. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "atualizado_em", insertable = false, updatable = false)
    private LocalDateTime atualizadoEm;
}
