package br.com.buscapeca.buscapecaapi.busca.repository;

import br.com.buscapeca.buscapecaapi.busca.dto.LojaResumo;
import br.com.buscapeca.buscapecaapi.busca.dto.ResultadoBusca;
import br.com.buscapeca.buscapecaapi.peca.enums.CondicaoPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.OrigemPeca;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Consulta da busca (GET /api/busca) em SQL montado conforme os filtros preenchidos.
 *
 * So le as tabelas de peca, loja, endereco, promocao e foto_peca: nao grava em nenhuma
 * delas. A regra do ADR-003 proibe gravar na tabela de outro modulo ou usar o repositorio
 * dele; uma consulta de leitura dentro do proprio modulo busca fica dentro da regra.
 *
 * Aparece so anuncio com status ATIVO de loja ativa.
 */
@Repository
public class BuscaRepository {

    /** Menor preco promocional valendo agora, ou nulo. */
    private static final String PRECO_PROMOCIONAL = """
            (SELECT MIN(pr.preco_promocional) FROM promocao pr
              WHERE pr.id_peca = p.id_peca AND pr.ativa = TRUE
                AND NOW() BETWEEN pr.data_inicio AND pr.data_fim)""";

    /** Preco que o comprador paga: o promocional, quando houver, senao o normal. */
    private static final String PRECO_EFETIVO = "COALESCE(" + PRECO_PROMOCIONAL + ", p.preco)";

    private static final String FOTO_PRINCIPAL = """
            (SELECT f.url_imagem FROM foto_peca f
              WHERE f.id_peca = p.id_peca
              ORDER BY f.principal DESC, f.ordem LIMIT 1)""";

    private static final String FROM = """
             FROM peca p
             JOIN loja l     ON l.id_loja = p.id_loja
             JOIN endereco e ON e.id_endereco = l.id_endereco
            """;

    private static final String MATCH = "MATCH(p.titulo, p.descricao) AGAINST (:textoFulltext IN BOOLEAN MODE)";

    private static final String CODIGO_EXATO =
            "(p.codigo_oem = :textoCodigo OR p.codigo_ean = :textoCodigo OR p.codigo_sku = :textoSku)";

    private final NamedParameterJdbcTemplate jdbc;

    public BuscaRepository(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    /** Uma pagina de resultados, ja na ordem pedida. */
    public List<ResultadoBusca> buscar(CriteriosBusca c, int limite, long deslocamento) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = montarWhere(c, params);
        params.addValue("limite", limite);
        params.addValue("deslocamento", deslocamento);

        StringBuilder sql = new StringBuilder("""
                SELECT p.id_peca, p.titulo, p.codigo_oem, p.codigo_sku, p.codigo_ean, p.marca_peca,
                       p.preco, p.condicao, p.origem, p.quantidade_estoque, p.pronta_entrega,
                       p.prazo_expedicao_dias,
                       l.id_loja, l.nome_loja, l.avaliacao_media, e.cidade, e.estado,
                """);
        sql.append("       ").append(PRECO_PROMOCIONAL).append(" AS preco_promocional,\n");
        sql.append("       ").append(PRECO_EFETIVO).append(" AS preco_efetivo,\n");
        sql.append("       ").append(FOTO_PRINCIPAL).append(" AS foto_url");
        if (c.textoFulltext() != null) {
            sql.append(",\n       ").append(MATCH).append(" AS relevancia");
        }
        if (c.temTexto()) {
            sql.append(",\n       ").append(CODIGO_EXATO).append(" AS codigo_exato");
        }
        sql.append('\n').append(FROM).append(where).append('\n');
        sql.append(" ORDER BY ").append(ordenacao(c)).append('\n');
        sql.append(" LIMIT :limite OFFSET :deslocamento");

        return jdbc.query(sql.toString(), params, LINHA);
    }

    /** Total de anuncios e de lojas diferentes com os mesmos filtros. */
    public Totais contar(CriteriosBusca c) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = "SELECT COUNT(*) AS total, COUNT(DISTINCT p.id_loja) AS lojas\n" + FROM + montarWhere(c, params);
        return jdbc.queryForObject(sql, params,
                (rs, i) -> new Totais(rs.getLong("total"), rs.getLong("lojas")));
    }

    public record Totais(long registros, long lojas) {
    }

    private static String montarWhere(CriteriosBusca c, MapSqlParameterSource params) {
        List<String> condicoes = new ArrayList<>();
        condicoes.add("p.status = 'ATIVO'");
        condicoes.add("l.ativa = TRUE");

        if (c.temTexto()) {
            List<String> alternativas = new ArrayList<>();
            if (c.textoFulltext() != null) {
                alternativas.add(MATCH);
                params.addValue("textoFulltext", c.textoFulltext());
            } else {
                // Palavras curtas demais para o indice FULLTEXT (menos de 3 letras)
                alternativas.add("p.titulo LIKE :textoLike");
            }
            alternativas.add("p.marca_peca LIKE :textoLike");
            alternativas.add(CODIGO_EXATO);
            params.addValue("textoLike", c.textoLike());
            params.addValue("textoCodigo", c.textoCodigo());
            params.addValue("textoSku", c.textoSku());
            condicoes.add("(" + String.join(" OR ", alternativas) + ")");
        }
        if (c.codigo() != null) {
            condicoes.add("(p.codigo_oem = :codigo OR p.codigo_ean = :codigo OR p.codigo_sku = :codigoSku)");
            params.addValue("codigo", c.codigo());
            params.addValue("codigoSku", c.codigoSku());
        }
        if (c.idCategoria() != null) {
            condicoes.add("""
                    (p.id_categoria = :categoria
                     OR p.id_categoria IN (SELECT sub.id_categoria FROM categoria sub
                                            WHERE sub.id_categoria_pai = :categoria))""");
            params.addValue("categoria", c.idCategoria());
        }
        if (!c.veiculos().isEmpty()) {
            condicoes.add("""
                    EXISTS (SELECT 1 FROM compatibilidade cp
                             WHERE cp.id_peca = p.id_peca AND cp.id_veiculo IN (:veiculos))""");
            params.addValue("veiculos", c.veiculos());
        }
        if (!c.tags().isEmpty()) {
            condicoes.add("""
                    EXISTS (SELECT 1 FROM peca_tag pt
                             WHERE pt.id_peca = p.id_peca AND pt.id_tag IN (:tags))""");
            params.addValue("tags", c.tags());
        }
        if (c.condicao() != null) {
            condicoes.add("p.condicao = :condicao");
            params.addValue("condicao", c.condicao().name());
        }
        if (c.origem() != null) {
            condicoes.add("p.origem = :origem");
            params.addValue("origem", c.origem().name());
        }
        if (Boolean.TRUE.equals(c.prontaEntrega())) {
            condicoes.add("p.pronta_entrega = TRUE");
        }
        if (c.formaPagamento() != null) {
            condicoes.add("""
                    EXISTS (SELECT 1 FROM loja_metodo_pagamento m
                             WHERE m.id_loja = l.id_loja AND m.metodo = :formaPagamento)""");
            params.addValue("formaPagamento", c.formaPagamento().name());
        }
        if (c.notaMinima() != null) {
            condicoes.add("l.avaliacao_media >= :notaMinima");
            params.addValue("notaMinima", c.notaMinima());
        }
        if (Boolean.TRUE.equals(c.retirada())) {
            condicoes.add("l.retirada_balcao = TRUE");
        }
        if (Boolean.TRUE.equals(c.freteGratis())) {
            condicoes.add("l.frete_gratis_raio_km IS NOT NULL AND l.frete_gratis_raio_km > 0");
        }
        if (c.precoMin() != null) {
            condicoes.add(PRECO_EFETIVO + " >= :precoMin");
            params.addValue("precoMin", c.precoMin());
        }
        if (c.precoMax() != null) {
            condicoes.add(PRECO_EFETIVO + " <= :precoMax");
            params.addValue("precoMax", c.precoMax());
        }
        return " WHERE " + String.join("\n   AND ", condicoes);
    }

    /** Sempre termina em p.id_peca, para a paginacao nao repetir nem pular itens. */
    private static String ordenacao(CriteriosBusca c) {
        return switch (c.ordenacao()) {
            case PRECO -> "preco_efetivo ASC, p.id_peca DESC";
            case PRECO_DESC -> "preco_efetivo DESC, p.id_peca DESC";
            case AVALIACAO -> "l.avaliacao_media DESC, preco_efetivo ASC, p.id_peca DESC";
            // DISTANCIA: sem latitude/longitude das lojas ainda (BP1-151), usa relevancia
            case RELEVANCIA, DISTANCIA -> relevancia(c);
        };
    }

    /** Codigo exato primeiro; depois a pontuacao do texto; sem texto, os mais novos. */
    private static String relevancia(CriteriosBusca c) {
        if (!c.temTexto()) {
            return "p.data_cadastro DESC, p.id_peca DESC";
        }
        String ordem = "codigo_exato DESC, ";
        if (c.textoFulltext() != null) {
            ordem += "relevancia DESC, ";
        }
        return ordem + "p.data_cadastro DESC, p.id_peca DESC";
    }

    private static final RowMapper<ResultadoBusca> LINHA = (rs, i) -> new ResultadoBusca(
            rs.getLong("id_peca"),
            rs.getString("titulo"),
            rs.getString("codigo_oem"),
            rs.getString("codigo_sku"),
            rs.getString("codigo_ean"),
            rs.getString("marca_peca"),
            rs.getBigDecimal("preco"),
            rs.getBigDecimal("preco_promocional"),
            CondicaoPeca.valueOf(rs.getString("condicao")),
            OrigemPeca.valueOf(rs.getString("origem")),
            rs.getInt("quantidade_estoque"),
            rs.getBoolean("pronta_entrega"),
            rs.getInt("prazo_expedicao_dias"),
            rs.getString("foto_url"),
            false,
            null,
            new LojaResumo(
                    rs.getLong("id_loja"),
                    rs.getString("nome_loja"),
                    rs.getBigDecimal("avaliacao_media"),
                    rs.getString("cidade"),
                    rs.getString("estado")));
}
