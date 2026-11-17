package br.com.buscapeca.buscapecaapi.loja.entity;

import br.com.buscapeca.buscapecaapi.loja.enums.MetodoPagamento;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Loja do usuario (BP1-55/67, ADR-007): uma por conta. Criar a loja da o papel VENDEDOR.
 * Exclusao logica: ativa = false.
 */
@Entity
@Table(name = "loja")
@Getter
@Setter
@NoArgsConstructor
public class Loja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_loja")
    private Long id;

    /** Dono da loja. Guardado so o id, como no Endereco. */
    @Column(name = "id_usuario", nullable = false, updatable = false)
    private Long idUsuario;

    /** Endereco da loja: um dos enderecos do dono (usuario/Endereco). */
    @Column(name = "id_endereco", nullable = false)
    private Long idEndereco;

    @Column(name = "nome_loja", nullable = false, length = 100)
    private String nomeLoja;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "descricao", columnDefinition = "text")
    private String descricao;

    /** So digitos, com DDD (V11). */
    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "avaliacao_media", nullable = false, precision = 2, scale = 1)
    private BigDecimal avaliacaoMedia = BigDecimal.ZERO;

    @Column(name = "vendedor_verificado", nullable = false)
    private boolean vendedorVerificado;

    @Column(name = "retirada_balcao", nullable = false)
    private boolean retiradaBalcao = true;

    /** Ex.: "Seg. a sex., 8h-18h" (V11; 120 caracteres desde a V14, como na tela). */
    @Column(name = "horario_retirada", length = 120)
    private String horarioRetirada;

    /**
     * Motoboy da propria loja / entrega rapida. A tela de criar loja chama de
     * "Motoboy proprio" e a de configuracoes de "entrega rapida": e a mesma opcao
     * (a V14 juntou as colunas motoboy_proprio e entrega_rapida).
     */
    @Column(name = "entrega_rapida", nullable = false)
    private boolean entregaRapida;

    /** Entrega combinada com o comprador (V11). */
    @Column(name = "entrega_combinar", nullable = false)
    private boolean entregaCombinar = true;

    @Column(name = "frete_gratis_raio_km", precision = 5, scale = 1)
    private BigDecimal freteGratisRaioKm;

    /** Valor minimo do pedido para frete gratis dentro do raio (V11). */
    @Column(name = "pedido_minimo_frete_gratis", precision = 10, scale = 2)
    private BigDecimal pedidoMinimoFreteGratis;

    @Column(name = "aceita_faturamento_cnpj", nullable = false)
    private boolean aceitaFaturamentoCnpj;

    @Column(name = "ativa", nullable = false)
    private boolean ativa = true;

    /** Preenchida pelo proprio MySQL (ON UPDATE CURRENT_TIMESTAMP). */
    @Column(name = "atualizado_em", insertable = false, updatable = false)
    private LocalDateTime atualizadoEm;

    /** Linhas da tabela loja_metodo_pagamento. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "loja_metodo_pagamento", joinColumns = @JoinColumn(name = "id_loja"))
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo", nullable = false, length = 20)
    private Set<MetodoPagamento> metodosPagamento = new HashSet<>();
}
