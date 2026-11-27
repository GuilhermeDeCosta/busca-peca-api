package br.com.buscapeca.buscapecaapi.pedido.mapper;

import br.com.buscapeca.buscapecaapi.pedido.dto.CompraResponse;
import br.com.buscapeca.buscapecaapi.pedido.dto.CompraResponse.ItemDaCompra;
import br.com.buscapeca.buscapecaapi.pedido.dto.CompraResponse.PedidoDaCompra;
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
