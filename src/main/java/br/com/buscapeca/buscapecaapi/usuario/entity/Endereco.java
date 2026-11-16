package br.com.buscapeca.buscapecaapi.usuario.entity;

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

/**
 * Endereco do usuario (BP1-60). Um usuario pode ter varios; so um e o principal.
 * A loja aponta para um destes enderecos (loja.id_endereco).
 */
@Entity
@Table(name = "endereco")
@Getter
@Setter
@NoArgsConstructor
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_endereco")
    private Long id;

    /** Dono do endereco. Guardado so o id, para nao carregar o usuario inteiro. */
    @Column(name = "id_usuario", nullable = false, updatable = false)
    private Long idUsuario;

    /** Apelido livre, ex.: "Oficina", "Casa" (V10). */
    @Column(name = "identificacao", length = 40)
    private String identificacao;

    @Column(name = "logradouro", nullable = false, length = 150)
    private String logradouro;

    @Column(name = "numero", nullable = false, length = 10)
    private String numero;

    @Column(name = "complemento", length = 80)
    private String complemento;

    @Column(name = "bairro", nullable = false, length = 80)
    private String bairro;

    @Column(name = "cidade", nullable = false, length = 80)
    private String cidade;

    @Column(name = "estado", nullable = false, columnDefinition = "char(2)")
    private String estado;

    /** So os 8 digitos, sem traco. */
    @Column(name = "cep", nullable = false, columnDefinition = "char(8)")
    private String cep;

    /** Preenchidas pela geolocalizacao do CEP; ficam nulas se o servico falhar (ADR-012). */
    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "principal", nullable = false)
    private boolean principal;
}