package br.com.buscapeca.buscapecaapi.pedido.mapper;

import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoDetalheResponse;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoDetalheResponse.Comprador;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoDetalheResponse.Envio;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoDetalheResponse.Item;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoDetalheResponse.Loja;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoDetalheResponse.Passo;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoRecebidoResponse;
import br.com.buscapeca.buscapecaapi.pedido.entity.Entrega;
import br.com.buscapeca.buscapecaapi.pedido.entity.ItemPedido;
import br.com.buscapeca.buscapecaapi.pedido.entity.Pedido;
import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import br.com.buscapeca.buscapecaapi.usuario.dto.EnderecoResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Conversao do pedido para as telas do comprador e da Minha Loja. */
@Component
public class PedidoMapper {

    public static final String VISAO_COMPRADOR = "COMPRADOR";
    public static final String VISAO_VENDEDOR = "VENDEDOR";

    /**
     * @param pecas      idPeca -> dados atuais da peca (titulo, codigos, foto, estoque)
     * @param endereco   endereco de entrega, ou null na retirada
     * @param comprador  dados de contato de quem comprou
     */
    public PedidoDetalheResponse detalhe(Pedido p, String visao, List<ItemPedido> itens, Map<Long, PecaParaCompra> pecas,
                                         Comprador comprador, String nomeLoja, EnderecoResponse endereco,
                                         Entrega entrega) {
        boolean comprando = VISAO_COMPRADOR.equals(visao);
        return new PedidoDetalheResponse(
                p.getId(),
                p.codigo(),
                visao,
                p.getStatus(),
                p.getDataPedido(),
                p.getAtualizadoEm(),
                p.getModalidadeEntrega(),
                p.getValorItens(),
                p.getValorFrete(),
                p.getObservacao() == null ? "" : p.getObservacao(),
                p.getMotivoRecusa(),
                p.getMotivoCancelamento(),
                null,
                comprador,
                new Loja(p.getIdLoja(), nomeLoja),
                endereco == null ? null : enderecoEmUmaLinha(endereco),
                // A loja confere o codigo no balcao: so o comprador o recebe
                comprando && p.getModalidadeEntrega() == ModalidadeEntrega.RETIRADA ? p.getCodigoRetirada() : null,
                itens.stream().map(i -> item(i, pecas.get(i.getIdPeca()))).toList(),
                historico(p),
                entrega == null ? null
                        : new Envio(entrega.getModalidade(), entrega.getTransportadora(), entrega.getCodigoRastreio()));
    }

    public PedidoRecebidoResponse recebido(Pedido p, String comprador, String resumoItens) {
        return new PedidoRecebidoResponse(p.getId(), p.codigo(), comprador, resumoItens, p.getDataPedido(),
                p.getValorItens(), p.getStatus(), p.getModalidadeEntrega(), null);
    }

    /**
     * "1 alternador Bosch" ou "2 filtros de ar e mais 1 item". O titulo vai como esta no
     * anuncio; o front ja mostra assim nos dados simulados.
     */
    public String resumoItens(List<ItemPedido> itens, Map<Long, PecaParaCompra> pecas) {
        if (itens.isEmpty()) {
            return "";
        }
        ItemPedido primeiro = itens.get(0);
        PecaParaCompra peca = pecas.get(primeiro.getIdPeca());
        String texto = primeiro.getQuantidade() + " " + (peca == null ? "peça" : peca.titulo());
        int outros = itens.size() - 1;
        if (outros > 0) {
            texto += " e mais " + outros + (outros == 1 ? " item" : " itens");
        }
        return texto;
    }

    private static Item item(ItemPedido i, PecaParaCompra peca) {
        return new Item(
                i.getIdPeca(),
                peca == null ? null : peca.titulo(),
                peca == null ? null : peca.codigoSku(),
                peca == null ? null : peca.codigoOem(),
                peca == null ? null : peca.marcaPeca(),
                peca == null ? null : peca.fotoUrl(),
                i.getQuantidade(),
                i.getPrecoUnitario(),
                peca == null ? null : peca.quantidadeEstoque());
    }

    private static List<Passo> historico(Pedido p) {
        List<Passo> passos = new ArrayList<>();
        passos.add(new Passo(StatusPedido.AGUARDANDO_ACEITE, p.getDataPedido()));
        if (p.getStatus() != StatusPedido.AGUARDANDO_ACEITE) {
            passos.add(new Passo(p.getStatus(), p.getAtualizadoEm()));
        }
        return passos;
    }

    /** Ex.: "Av. dos Imigrantes, 455 • Centro • Valinhos/SP • CEP 13270-000". */
    private static String enderecoEmUmaLinha(EnderecoResponse e) {
        StringBuilder linha = new StringBuilder(e.logradouro());
        if (e.numero() != null && !e.numero().isBlank()) {
            linha.append(", ").append(e.numero());
        }
        if (e.complemento() != null && !e.complemento().isBlank()) {
            linha.append(" - ").append(e.complemento());
        }
        if (e.bairro() != null && !e.bairro().isBlank()) {
            linha.append(" • ").append(e.bairro());
        }
        linha.append(" • ").append(e.cidade()).append('/').append(e.estado());
        String cep = e.cep();
        if (cep != null && cep.length() == 8) {
            linha.append(" • CEP ").append(cep, 0, 5).append('-').append(cep.substring(5));
        }
        return linha.toString();
    }
}
