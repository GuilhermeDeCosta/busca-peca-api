package br.com.buscapeca.buscapecaapi.pedido.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Consultas de leitura nas tabelas pedido e item_pedido (V7), usadas por outros modulos
 * pelo PedidoService. Fica em SQL enquanto o modulo de pedidos nao tem as entidades;
 * quando elas chegarem, esta consulta pode virar um metodo do repositorio JPA.
 */
@Repository
public class PedidoConsultaRepository {

    /** Pedidos que ainda podem gerar entrega: a peca nao pode sumir no meio deles. */
    static final List<String> STATUS_EM_ANDAMENTO =
            List.of("AGUARDANDO_ACEITE", "ACEITO", "PRONTO_RETIRADA", "EM_ENTREGA");

    private static final String PEDIDOS_EM_ANDAMENTO_DA_PECA = """
            SELECT DISTINCT p.id_pedido
              FROM pedido p
              JOIN item_pedido i ON i.id_pedido = p.id_pedido
             WHERE i.id_peca = :idPeca
               AND p.status IN (:status)
             ORDER BY p.id_pedido
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public PedidoConsultaRepository(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    /** Ids dos pedidos em andamento que tem a peca, do mais antigo para o mais novo. */
    public List<Long> pedidosEmAndamentoComPeca(Long idPeca) {
        return jdbc.queryForList(PEDIDOS_EM_ANDAMENTO_DA_PECA,
                Map.of("idPeca", idPeca, "status", STATUS_EM_ANDAMENTO), Long.class);
    }
}
