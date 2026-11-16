package br.com.buscapeca.buscapecaapi.integracao.geolocalizacao;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Consulta de CEP e coordenadas, isolada atras de uma interface (ADR-012).
 * Os modulos de negocio conhecem so esta interface.
 * As implementacoes nunca lancam excecao: em caso de falha devolvem vazio (timeout de 3 s).
 */
public interface GeolocalizacaoService {

    /** Rua, bairro, cidade e UF do CEP. Vazio se o CEP nao existe ou o servico falhou. */
    Optional<EnderecoPorCep> buscarEndereco(String cep);

    /** Latitude e longitude do CEP. Vazio se nao der para descobrir; o endereco e salvo sem elas. */
    Optional<Coordenadas> buscarCoordenadas(String cep);

    record EnderecoPorCep(String cep, String logradouro, String bairro, String cidade, String estado) {
    }

    record Coordenadas(BigDecimal latitude, BigDecimal longitude) {
    }
}
