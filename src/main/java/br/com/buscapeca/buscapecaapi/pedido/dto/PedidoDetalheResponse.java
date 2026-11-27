package br.com.buscapeca.buscapecaapi.pedido.dto;

import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEnvio;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /api/pedidos/{id} e a resposta de toda mudanca de status (BP1-26, 27, 29, 30, 33).
 * O mesmo formato serve ao comprador e a loja; o que muda e o que cada um ve:
 * a loja nunca recebe codigoRetirada (ele e conferido no balcao).
 *
 * @param visao          COMPRADOR ou VENDEDOR: quem esta vendo
 * @param enderecoEntrega so na ENTREGA, em uma linha
 * @param codigoRetirada so para o comprador e so na RETIRADA
 * @param historico      linha do tempo: a criacao e a ultima mudanca (o banco ainda nao guarda cada passo)
 * @param entrega        dados do envio, depois de "enviar"
 * @param idConversa     sempre null por enquanto (BP1-28)
 */
public record PedidoDetalheResponse(
        Long idPedido,
        String codigo,
        String visao,
        StatusPedido status,
        LocalDateTime dataPedido,
        LocalDateTime atualizadoEm,
        ModalidadeEntrega modalidadeEntrega,
        BigDecimal valorItens,
        BigDecimal valorFrete,
        String observacao,
        String motivoRecusa,
        String motivoCancelamento,
        Long idConversa,
        Comprador comprador,
        Loja loja,
        String enderecoEntrega,
        String codigoRetirada,
        List<Item> itens,
        List<Passo> historico,
        Envio entrega
) {

    /** cidade e estado: do endereco de entrega ou, na retirada, do endereco principal. */
    public record Comprador(String nome, String cidade, String estado, String nomeContato, String telefone) {
    }

    public record Loja(Long idLoja, String nomeLoja) {
    }

    /**
     * @param precoUnitario o do momento da compra
     * @param estoqueAtual  estoque de agora, para a loja decidir o aceite
     */
    public record Item(Long idPeca, String titulo, String sku, String codigoOem, String marcaPeca, String fotoUrl,
                       int quantidade, BigDecimal precoUnitario, Integer estoqueAtual) {
    }

    public record Passo(StatusPedido status, LocalDateTime data) {
    }

    public record Envio(ModalidadeEnvio modalidade, String transportadora, String codigoRastreio) {
    }
}
