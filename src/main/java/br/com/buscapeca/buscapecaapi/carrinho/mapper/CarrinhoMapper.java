package br.com.buscapeca.buscapecaapi.carrinho.mapper;

import br.com.buscapeca.buscapecaapi.carrinho.dto.CarrinhoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.CarrinhoResponse.ItemCarrinhoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.CarrinhoResponse.LojaCarrinhoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.CarrinhoResponse.LojaDoCarrinho;
import br.com.buscapeca.buscapecaapi.carrinho.dto.CarrinhoResponse.PecaDoItem;
import br.com.buscapeca.buscapecaapi.carrinho.entity.ItemCarrinho;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaResumoResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Conversao dos itens do carrinho, com as pecas e lojas vindas dos outros modulos, para os DTOs. */
@Component
public class CarrinhoMapper {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public CarrinhoResponse vazio() {
        return new CarrinhoResponse(List.of(), 0, ZERO);
    }

    /**
     * Um item com os dados atuais da peca.
     *
     * @param lojaAtiva a loja da peca ainda vende; se nao, o item fica indisponivel
     */
    public ItemCarrinhoResponse item(ItemCarrinho item, PecaParaCompra peca, boolean lojaAtiva) {
        boolean disponivel = peca.disponivel() && lojaAtiva;
        boolean estoqueSuficiente = disponivel && item.getQuantidade() <= peca.quantidadeEstoque();
        BigDecimal subtotal = peca.preco() == null
                ? ZERO
                : peca.preco().multiply(BigDecimal.valueOf(item.getQuantidade())).setScale(2, RoundingMode.HALF_UP);
        return new ItemCarrinhoResponse(
                item.getId(),
                item.getQuantidade(),
                peca.preco(),
                subtotal,
                estoqueSuficiente,
                disponivel,
                new PecaDoItem(
                        peca.idPeca(),
                        peca.titulo(),
                        peca.codigoOem(),
                        peca.codigoSku(),
                        peca.marcaPeca(),
                        peca.fotoUrl(),
                        peca.quantidadeEstoque(),
                        peca.prontaEntrega(),
                        peca.prazoExpedicaoDias()));
    }

    /**
     * Grupo de uma loja. O subtotal soma so os itens disponiveis: peca pausada ou
     * excluida continua na lista para o comprador remover, mas nao entra na conta.
     */
    public LojaCarrinhoResponse loja(Long idLoja, LojaResumoResponse resumo, List<ItemCarrinhoResponse> itens) {
        LojaDoCarrinho loja = resumo == null
                ? new LojaDoCarrinho(idLoja, null, false, false, false)
                : new LojaDoCarrinho(resumo.idLoja(), resumo.nomeLoja(), resumo.retiradaBalcao(),
                        resumo.entregaCombinar(), resumo.motoboyProprio());
        BigDecimal subtotal = itens.stream()
                .filter(ItemCarrinhoResponse::disponivel)
                .map(ItemCarrinhoResponse::subtotal)
                .reduce(ZERO, BigDecimal::add);
        return new LojaCarrinhoResponse(loja, subtotal, itens);
    }

    public CarrinhoResponse carrinho(List<LojaCarrinhoResponse> lojas) {
        int quantidadeTotal = lojas.stream()
                .flatMap(l -> l.itens().stream())
                .mapToInt(ItemCarrinhoResponse::quantidade)
                .sum();
        BigDecimal total = lojas.stream()
                .map(LojaCarrinhoResponse::subtotal)
                .reduce(ZERO, BigDecimal::add);
        return new CarrinhoResponse(lojas, quantidadeTotal, total);
    }
}
