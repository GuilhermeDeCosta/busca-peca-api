package br.com.buscapeca.buscapecaapi.peca.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Leituras das tabelas promocao e foto_peca (V4), que ainda nao tem entidade.
 * As mesmas regras da busca: o menor preco promocional valendo agora e a foto
 * principal (ou a primeira pela ordem).
 */
@Repository
public class PecaVitrineRepository {

    private static final String PRECOS_PROMOCIONAIS = """
            SELECT pr.id_peca, MIN(pr.preco_promocional) AS preco
              FROM promocao pr
             WHERE pr.id_peca IN (:ids)
               AND pr.ativa = TRUE
               AND NOW() BETWEEN pr.data_inicio AND pr.data_fim
             GROUP BY pr.id_peca
            """;

    private static final String FOTOS_PRINCIPAIS = """
            SELECT f.id_peca, f.url_imagem
              FROM foto_peca f
             WHERE f.id_peca IN (:ids)
             ORDER BY f.id_peca, f.principal DESC, f.ordem
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public PecaVitrineRepository(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    /** idPeca -> preco promocional vigente. Pecas sem promocao ficam de fora. */
    public Map<Long, BigDecimal> precosPromocionais(Collection<Long> idsPeca) {
        Map<Long, BigDecimal> precos = new HashMap<>();
        if (idsPeca.isEmpty()) {
            return precos;
        }
        jdbc.query(PRECOS_PROMOCIONAIS, Map.of("ids", idsPeca),
                rs -> { precos.put(rs.getLong("id_peca"), rs.getBigDecimal("preco")); });
        return precos;
    }

    /** idPeca -> URL da foto principal. Pecas sem foto ficam de fora. */
    public Map<Long, String> fotosPrincipais(Collection<Long> idsPeca) {
        Map<Long, String> fotos = new HashMap<>();
        if (idsPeca.isEmpty()) {
            return fotos;
        }
        // Vem ordenado com a principal primeiro: fica a primeira de cada peca
        jdbc.query(FOTOS_PRINCIPAIS, Map.of("ids", idsPeca),
                rs -> { fotos.putIfAbsent(rs.getLong("id_peca"), rs.getString("url_imagem")); });
        return fotos;
    }
}
