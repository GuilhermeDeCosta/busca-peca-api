package br.com.buscapeca.buscapecaapi.pedido.mapper;

import br.com.buscapeca.buscapecaapi.pedido.dto.CompraResponse;
import br.com.buscapeca.buscapecaapi.pedido.dto.CompraResponse.ItemDaCompra;
import br.com.buscapeca.buscapecaapi.pedido.dto.CompraResponse.PedidoDaCompra;
import br.com.buscapeca.buscapecaapi.pedido.dto.MinhasComprasResponse.CompraDoHistorico;
import br.com.buscapeca.buscapecaapi.pedido.dto.MinhasComprasResponse.ItemDoHistorico;
import br.com.buscapeca.buscapecaapi.pedido.dto.MinhasComprasResponse.PedidoDoHistorico;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import br.com.buscapeca.buscapecaapi.pedido.entity.Compra;
import br.com.buscapeca.buscapecaapi.pedido.entity.ItemPedido;
import br.com.buscapeca.buscapecaapi.pedido.entity.Pedido;
import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Conversao compra -> DTO. Nomes de loja e titulos de peca vem dos servicos desses modulos. */
@Component
public class CompraMapper {

    /**
     * @param itensPorPedido idPedido -> itens
     * @param titulos        idPeca -> titulo
     * @param nomesLoja      idLoja -> nome
     */
    public CompraResponse paraResponse(Compra compra, List<Pedido> pedidos, Map<Long, List<ItemPedido>> itensPorPedido,
                                       Map<Long, String> titulos, Map<Long, String> nomesLoja) {
        List<PedidoDaCompra> lista = pedidos.stream()
                .map(p -> pedido(p, itensPorPedido.getOrDefault(p.getId(), List.of()), titulos, nomesLoja))
                .toList();
        return new CompraResponse(compra.getId(), compra.getDataCompra(), compra.getValorTotal(),
                compra.getIdEnderecoEntrega(), lista);
    }

    /** Linha de Meus pedidos (GET /compras). */
    public CompraDoHistorico compraDoHistorico(Compra compra, List<PedidoDoHistorico> pedidos) {
        return new CompraDoHistorico(compra.getId(), compra.getDataCompra(), compra.getValorTotal(), pedidos);
    }

    /**
     * @param pecas idPeca -> dados atuais, so para o titulo; peca que sumiu fica com titulo nulo
     */
    public PedidoDoHistorico pedidoDoHistorico(Pedido p, String nomeLoja, String resumoItens,
                                               List<ItemPedido> itens, Map<Long, PecaParaCompra> pecas) {
        return new PedidoDoHistorico(
                p.getId(),
                p.codigo(),
                nomeLoja,
                p.getStatus(),
                p.getModalidadeEntrega(),
                p.getModalidadeEntrega() == ModalidadeEntrega.RETIRADA ? p.getCodigoRetirada() : null,
                p.getValorItens(),
                resumoItens,
                itens.stream()
                        .map(i -> {
                            PecaParaCompra peca = pecas.get(i.getIdPeca());
                            return new ItemDoHistorico(peca == null ? null : peca.titulo(), i.getQuantidade());
                        })
                        .toList());
    }

    private static PedidoDaCompra pedido(Pedido p, List<ItemPedido> itens, Map<Long, String> titulos,
                                         Map<Long, String> nomesLoja) {
        return new PedidoDaCompra(
                p.getId(),
                p.codigo(),
                p.getIdLoja(),
                nomesLoja.get(p.getIdLoja()),
                p.getStatus(),
                p.getModalidadeEntrega(),
                p.getObservacao() == null ? "" : p.getObservacao(),
                // O codigo so serve no balcao: na entrega ele nao aparece
                p.getModalidadeEntrega() == ModalidadeEntrega.RETIRADA ? p.getCodigoRetirada() : null,
                p.getValorItens(),
                itens.stream()
                        .map(i -> new ItemDaCompra(i.getIdPeca(), titulos.get(i.getIdPeca()), i.getQuantidade(),
                                i.getPrecoUnitario()))
                        .toList());
    }
}
