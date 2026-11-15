package br.com.buscapeca.buscapecaapi.integracao.geolocalizacao;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Implementacao falsa do perfil dev (ADR-012): responde so os CEPs abaixo,
 * sem chamar nenhum servico externo. Os outros CEPs voltam vazios,
 * e o usuario digita o endereco a mao, como no fluxo real de falha.
 */
@Service
@Profile("!prod")
public class GeolocalizacaoServiceFalso implements GeolocalizacaoService {

    private static final Map<String, EnderecoPorCep> ENDERECOS = Map.of(
            "13010110", new EnderecoPorCep("13010110", "Rua das Oficinas", "Centro", "Campinas", "SP"),
            "01310100", new EnderecoPorCep("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP"),
            "30130010", new EnderecoPorCep("30130010", "Praça Sete de Setembro", "Centro", "Belo Horizonte", "MG"));

    private static final Map<String, Coordenadas> COORDENADAS = Map.of(
            "13010110", new Coordenadas(new BigDecimal("-22.9056390"), new BigDecimal("-47.0608080")),
            "01310100", new Coordenadas(new BigDecimal("-23.5613500"), new BigDecimal("-46.6563500")),
            "30130010", new Coordenadas(new BigDecimal("-19.9191200"), new BigDecimal("-43.9386600")));

    @Override
    public Optional<EnderecoPorCep> buscarEndereco(String cep) {
        return Optional.ofNullable(ENDERECOS.get(cep));
    }

    @Override
    public Optional<Coordenadas> buscarCoordenadas(String cep) {
        return Optional.ofNullable(COORDENADAS.get(cep));
    }
}
