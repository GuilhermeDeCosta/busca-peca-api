package br.com.buscapeca.buscapecaapi.integracao.geolocalizacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

/**
 * Consulta real de CEP no ViaCEP, perfil prod. Timeout de 3 s (ADR-012):
 * se o ViaCEP falhar, devolve vazio e o usuario digita o endereco.
 */
@Service
@Profile("prod")
public class GeolocalizacaoServiceViaCep implements GeolocalizacaoService {

    private static final Logger log = LoggerFactory.getLogger(GeolocalizacaoServiceViaCep.class);
    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(3);

    private final RestClient http;

    public GeolocalizacaoServiceViaCep() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(TEMPO_LIMITE);
        fabrica.setReadTimeout(TEMPO_LIMITE);
        this.http = RestClient.builder()
                .baseUrl("https://viacep.com.br/ws")
                .requestFactory(fabrica)
                .build();
    }

    @Override
    public Optional<EnderecoPorCep> buscarEndereco(String cep) {
        try {
            RespostaViaCep r = http.get().uri("/{cep}/json/", cep).retrieve().body(RespostaViaCep.class);
            // CEP inexistente: o ViaCEP responde { "erro": "true" }, sem localidade
            if (r == null || r.localidade() == null) {
                return Optional.empty();
            }
            return Optional.of(new EnderecoPorCep(cep, r.logradouro(), r.bairro(), r.localidade(), r.uf()));
        } catch (RestClientException e) {
            log.warn("ViaCEP indisponivel para o CEP {}: {}", cep, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * O ViaCEP nao informa coordenadas. Ate o time escolher um servico de
     * geocodificacao, o endereco e salvo sem elas (alternativa do ADR-012).
     */
    @Override
    public Optional<Coordenadas> buscarCoordenadas(String cep) {
        return Optional.empty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaViaCep(String cep, String logradouro, String bairro, String localidade, String uf) {
    }
}
