package br.com.buscapeca.buscapecaapi.pedido.dto;

import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resposta de GET /api/compras: historico do comprador (Meus pedidos, A10; BP1-20, BP1-27).
 * Mesmos campos da PaginaResposta (pagina comeca em 0) mais o resumo dos cartoes do topo.
 *
 * @param resumo conta todos os pedidos do comprador, sem os filtros da busca
 */
public record MinhasComprasResponse(
        List<CompraDoHistorico> itens,
        int pagina,
        int tamanho,
        long totalRegistros,
        int totalPaginas,
        Resumo resumo
) {

    /**
     * Uma compra com os pedidos que passaram nos filtros.
     *
     * @param total valor da compra inteira, mesmo quando algum pedido ficou fora do filtro
     */
    public record CompraDoHistorico(
            Long idCompra,
            LocalDateTime criadaEm,
            BigDecimal total,
            List<PedidoDoHistorico> pedidos
    ) {
    }

    /**
     * @param codigoRetirada so na RETIRADA; o comprador mostra no balcao (BP1-29)
     * @param resumoItens    ex.: "1 farol" ou "2 filtros de ar e mais 1 item"
     */
    public record PedidoDoHistorico(
            Long idPedido,
            String codigo,
            String nomeLoja,
            StatusPedido status,
            ModalidadeEntrega modalidade,
            String codigoRetirada,
            BigDecimal subtotal,
            String resumoItens,
            List<ItemDoHistorico> itens
    ) {
    }

    public record ItemDoHistorico(String titulo, int quantidade) {
    }

    /**
     * @param atencao   PRONTO_RETIRADA ou EM_ENTREGA: ir buscar ou confirmar o recebimento
     * @param andamento AGUARDANDO_ACEITE, ACEITO ou EM_PREPARACAO
     * @param retirada  PRONTO_RETIRADA
     */
    public record Resumo(long atencao, long andamento, long retirada) {
    }
}
