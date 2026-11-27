package br.com.buscapeca.buscapecaapi.carrinho.service;

import br.com.buscapeca.buscapecaapi.carrinho.dto.AdicionarItemRequest;
import br.com.buscapeca.buscapecaapi.carrinho.dto.AlterarQuantidadeRequest;
import br.com.buscapeca.buscapecaapi.carrinho.dto.CarrinhoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.CarrinhoResponse.ItemCarrinhoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.CarrinhoResponse.LojaCarrinhoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.FreteRequest;
import br.com.buscapeca.buscapecaapi.carrinho.dto.FreteResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.ItemAdicionadoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.entity.Carrinho;
import br.com.buscapeca.buscapecaapi.carrinho.entity.ItemCarrinho;
import br.com.buscapeca.buscapecaapi.carrinho.mapper.CarrinhoMapper;
import br.com.buscapeca.buscapecaapi.carrinho.repository.CarrinhoRepository;
import br.com.buscapeca.buscapecaapi.carrinho.repository.ItemCarrinhoRepository;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.ConflitoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.integracao.frete.FreteService;
import br.com.buscapeca.buscapecaapi.integracao.geolocalizacao.GeolocalizacaoService;
import br.com.buscapeca.buscapecaapi.integracao.geolocalizacao.GeolocalizacaoService.EnderecoPorCep;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaResumoResponse;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import br.com.buscapeca.buscapecaapi.peca.service.PecaCompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Carrinho com itens de varias lojas e frete por loja (BP1-22, 23). Sem banco: tudo
 * simulado, com o CarrinhoMapper de verdade para conferir as contas.
 */
@ExtendWith(MockitoExtension.class)
class CarrinhoServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_CARRINHO = 5L;
    private static final Long LOJA_A = 7L;
    private static final Long LOJA_B = 8L;

    @Mock
    private CarrinhoRepository carrinhoRepository;

    @Mock
    private ItemCarrinhoRepository itemRepository;

    @Mock
    private PecaCompraService pecaCompraService;

    @Mock
    private LojaService lojaService;

    @Mock
    private GeolocalizacaoService geolocalizacaoService;

    @Mock
    private FreteService freteService;

    private CarrinhoService service;

    @BeforeEach
    void preparar() {
        service = new CarrinhoService(carrinhoRepository, itemRepository, pecaCompraService, lojaService,
                geolocalizacaoService, freteService, new CarrinhoMapper());
    }

    // ----- Consultar -----

    @Test
    @DisplayName("Quem nunca adicionou nada recebe o carrinho vazio")
    void obterSemCarrinho() {
        when(carrinhoRepository.findByIdUsuario(ID_USUARIO)).thenReturn(Optional.empty());

        CarrinhoResponse carrinho = service.obter(ID_USUARIO);

        assertThat(carrinho.lojas()).isEmpty();
        assertThat(carrinho.quantidadeTotal()).isZero();
        assertThat(carrinho.total()).isEqualByComparingTo("0");
        verifyNoInteractions(pecaCompraService, lojaService);
    }

    @Test
    @DisplayName("Agrupa por loja, na ordem em que cada loja entrou, com subtotal e total")
    void obterAgrupaPorLoja() {
        temCarrinho();
        temItens(item(1L, 10L, 2), item(2L, 11L, 1), item(3L, 12L, 1));
        temPecas(peca(10L, LOJA_A, "100.00", 5, true), peca(11L, LOJA_B, "50.50", 5, true),
                peca(12L, LOJA_A, "20.00", 5, true));
        temLojas(loja(LOJA_A, true), loja(LOJA_B, true));

        CarrinhoResponse carrinho = service.obter(ID_USUARIO);

        assertThat(carrinho.lojas()).extracting(l -> l.loja().idLoja()).containsExactly(LOJA_A, LOJA_B);
        LojaCarrinhoResponse lojaA = carrinho.lojas().get(0);
        assertThat(lojaA.itens()).extracting(ItemCarrinhoResponse::idItem).containsExactly(1L, 3L);
        assertThat(lojaA.itens().get(0).subtotal()).isEqualByComparingTo("200.00");
        assertThat(lojaA.subtotal()).isEqualByComparingTo("220.00");
        assertThat(carrinho.lojas().get(1).subtotal()).isEqualByComparingTo("50.50");
        assertThat(carrinho.total()).isEqualByComparingTo("270.50");
        assertThat(carrinho.quantidadeTotal()).isEqualTo(4);
    }

    @Test
    @DisplayName("Anuncio pausado continua na lista, indisponivel e fora do total")
    void obterItemPausado() {
        temCarrinho();
        temItens(item(1L, 10L, 1), item(2L, 11L, 1));
        temPecas(peca(10L, LOJA_A, "100.00", 5, true), peca(11L, LOJA_A, "30.00", 5, false));
        temLojas(loja(LOJA_A, true));

        CarrinhoResponse carrinho = service.obter(ID_USUARIO);

        ItemCarrinhoResponse pausado = carrinho.lojas().get(0).itens().get(1);
        assertThat(pausado.disponivel()).isFalse();
        assertThat(pausado.estoqueSuficiente()).isFalse();
        assertThat(carrinho.total()).isEqualByComparingTo("100.00");
        assertThat(carrinho.quantidadeTotal()).isEqualTo(2);
    }

    @Test
    @DisplayName("Loja inativa deixa os itens dela indisponiveis")
    void obterLojaInativa() {
        temCarrinho();
        temItens(item(1L, 10L, 1));
        temPecas(peca(10L, LOJA_A, "100.00", 5, true));
        temLojas(loja(LOJA_A, false));

        ItemCarrinhoResponse item = service.obter(ID_USUARIO).lojas().get(0).itens().get(0);

        assertThat(item.disponivel()).isFalse();
        assertThat(item.estoqueSuficiente()).isFalse();
    }

    @Test
    @DisplayName("Estoque menor que a quantidade marca estoqueSuficiente = false")
    void obterEstoqueInsuficiente() {
        temCarrinho();
        temItens(item(1L, 10L, 4));
        temPecas(peca(10L, LOJA_A, "10.00", 3, true));
        temLojas(loja(LOJA_A, true));

        ItemCarrinhoResponse item = service.obter(ID_USUARIO).lojas().get(0).itens().get(0);

        assertThat(item.disponivel()).isTrue();
        assertThat(item.estoqueSuficiente()).isFalse();
    }

    // ----- Adicionar -----

    @Test
    @DisplayName("Primeiro item cria o carrinho")
    void adicionarCriaCarrinho() {
        temPeca(peca(10L, LOJA_A, "100.00", 5, true));
        temLojas(loja(LOJA_A, true));
        when(carrinhoRepository.findByIdUsuario(ID_USUARIO)).thenReturn(Optional.empty());
        when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(inv -> {
            Carrinho c = inv.getArgument(0);
            c.setId(ID_CARRINHO);
            return c;
        });
        when(itemRepository.findByIdCarrinhoOrderByIdAsc(ID_CARRINHO)).thenReturn(List.of());

        ItemAdicionadoResponse resposta = service.adicionar(ID_USUARIO, new AdicionarItemRequest(10L, 2));

        assertThat(resposta).isEqualTo(new ItemAdicionadoResponse(10L, 2, 2));
        ArgumentCaptor<ItemCarrinho> salvo = ArgumentCaptor.forClass(ItemCarrinho.class);
        verify(itemRepository).save(salvo.capture());
        assertThat(salvo.getValue().getIdCarrinho()).isEqualTo(ID_CARRINHO);
        assertThat(salvo.getValue().getQuantidade()).isEqualTo(2);
    }

    @Test
    @DisplayName("Peca que ja esta no carrinho soma na mesma linha; o total conta os outros itens")
    void adicionarSomaNaLinha() {
        temPeca(peca(10L, LOJA_A, "100.00", 5, true));
        temLojas(loja(LOJA_A, true));
        temCarrinho();
        ItemCarrinho existente = item(1L, 10L, 1);
        temItens(existente, item(2L, 11L, 3));

        ItemAdicionadoResponse resposta = service.adicionar(ID_USUARIO, new AdicionarItemRequest(10L, null));

        assertThat(resposta).isEqualTo(new ItemAdicionadoResponse(10L, 2, 5));
        verify(itemRepository).save(existente);
        assertThat(existente.getQuantidade()).isEqualTo(2);
    }

    @Test
    @DisplayName("Passar do estoque somando o carrinho da 409 ESTOQUE_INSUFICIENTE com a mensagem da tela")
    void adicionarAcimaDoEstoque() {
        temPeca(peca(10L, LOJA_A, "100.00", 3, true));
        temLojas(loja(LOJA_A, true));
        temCarrinho();
        temItens(item(1L, 10L, 2));

        assertThatThrownBy(() -> service.adicionar(ID_USUARIO, new AdicionarItemRequest(10L, 2)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Estoque insuficiente. A loja tem 3 em estoque e você já tem 2 no carrinho.")
                .extracting("codigo").isEqualTo("ESTOQUE_INSUFICIENTE");
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Anuncio pausado ou excluido nao entra no carrinho (404)")
    void adicionarPecaIndisponivel() {
        temPeca(peca(10L, LOJA_A, "100.00", 5, false));

        assertThatThrownBy(() -> service.adicionar(ID_USUARIO, new AdicionarItemRequest(10L, 1)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Este anúncio não está mais disponível.");
        verifyNoInteractions(carrinhoRepository, itemRepository);
    }

    @Test
    @DisplayName("Peca que nao existe da 404")
    void adicionarPecaInexistente() {
        when(pecaCompraService.buscar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adicionar(ID_USUARIO, new AdicionarItemRequest(99L, 1)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Peca de loja inativa nao entra no carrinho (404)")
    void adicionarLojaInativa() {
        temPeca(peca(10L, LOJA_A, "100.00", 5, true));
        temLojas(loja(LOJA_A, false));

        assertThatThrownBy(() -> service.adicionar(ID_USUARIO, new AdicionarItemRequest(10L, 1)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(carrinhoRepository, itemRepository);
    }

    // ----- Alterar, remover e esvaziar -----

    @Test
    @DisplayName("Alterar a quantidade devolve o carrinho recalculado")
    void alterarQuantidade() {
        temCarrinho();
        ItemCarrinho item = item(1L, 10L, 1);
        when(itemRepository.findByIdAndIdCarrinho(1L, ID_CARRINHO)).thenReturn(Optional.of(item));
        temPeca(peca(10L, LOJA_A, "25.00", 5, true));
        temLojas(loja(LOJA_A, true));
        temItens(item);
        temPecas(peca(10L, LOJA_A, "25.00", 5, true));

        CarrinhoResponse carrinho = service.alterarQuantidade(ID_USUARIO, 1L, new AlterarQuantidadeRequest(4));

        verify(itemRepository).save(item);
        assertThat(item.getQuantidade()).isEqualTo(4);
        assertThat(carrinho.total()).isEqualByComparingTo("100.00");
        assertThat(carrinho.quantidadeTotal()).isEqualTo(4);
    }

    @Test
    @DisplayName("Alterar acima do estoque da 409 com a mensagem da tela")
    void alterarAcimaDoEstoque() {
        temCarrinho();
        when(itemRepository.findByIdAndIdCarrinho(1L, ID_CARRINHO)).thenReturn(Optional.of(item(1L, 10L, 1)));
        temPeca(peca(10L, LOJA_A, "25.00", 3, true));
        temLojas(loja(LOJA_A, true));

        assertThatThrownBy(() -> service.alterarQuantidade(ID_USUARIO, 1L, new AlterarQuantidadeRequest(4)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Quantidade indisponível no estoque no momento. A loja tem 3.");
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Item de outro carrinho da 404")
    void alterarItemDeOutroUsuario() {
        temCarrinho();
        when(itemRepository.findByIdAndIdCarrinho(1L, ID_CARRINHO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.alterarQuantidade(ID_USUARIO, 1L, new AlterarQuantidadeRequest(2)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Este item não está mais no carrinho.");
    }

    @Test
    @DisplayName("Remover apaga o item do proprio carrinho")
    void remover() {
        temCarrinho();
        ItemCarrinho item = item(1L, 10L, 1);
        when(itemRepository.findByIdAndIdCarrinho(1L, ID_CARRINHO)).thenReturn(Optional.of(item));

        service.remover(ID_USUARIO, 1L);

        verify(itemRepository).delete(item);
    }

    @Test
    @DisplayName("Remover sem carrinho da 404")
    void removerSemCarrinho() {
        when(carrinhoRepository.findByIdUsuario(ID_USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remover(ID_USUARIO, 1L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Esvaziar apaga todos os itens")
    void esvaziar() {
        temCarrinho();

        service.esvaziar(ID_USUARIO);

        verify(itemRepository).apagarTodos(ID_CARRINHO);
    }

    // ----- Frete -----

    @Test
    @DisplayName("CEP com formato invalido ou zerado da 400 no campo cep")
    void freteCepInvalido() {
        for (String cep : List.of("1234", "00000-000", "abcdefgh")) {
            assertThatThrownBy(() -> service.calcularFrete(ID_USUARIO, new FreteRequest(cep)))
                    .isInstanceOf(DadosInvalidosException.class)
                    .hasMessage("CEP inválido ou área sem cobertura de entrega")
                    .extracting("campo").isEqualTo("cep");
        }
        verifyNoInteractions(geolocalizacaoService, freteService);
    }

    @Test
    @DisplayName("Sem cotacao, cada loja fica com o frete a combinar; a regiao vem do CEP")
    void freteACombinar() {
        when(geolocalizacaoService.buscarEndereco("13010110"))
                .thenReturn(Optional.of(new EnderecoPorCep("13010110", "Rua", "Centro", "Campinas", "SP")));
        temCarrinho();
        temItens(item(1L, 10L, 1), item(2L, 11L, 1));
        temPecas(peca(10L, LOJA_A, "100.00", 5, true), peca(11L, LOJA_B, "50.00", 5, true));
        temLojas(loja(LOJA_A, true), loja(LOJA_B, true));
        when(freteService.cotar(any(), eq("13010110"), any())).thenReturn(Optional.empty());

        FreteResponse frete = service.calcularFrete(ID_USUARIO, new FreteRequest("13010-110"));

        assertThat(frete.regiao()).isEqualTo(new FreteResponse.Regiao("Campinas", "SP"));
        assertThat(frete.lojas()).extracting(FreteResponse.LojaFrete::idLoja).containsExactly(LOJA_A, LOJA_B);
        assertThat(frete.lojas()).allSatisfy(l -> {
            assertThat(l.freteACombinar()).isTrue();
            assertThat(l.opcoes()).isEmpty();
            assertThat(l.retiradaBalcao()).isTrue();
        });
    }

    @Test
    @DisplayName("Loja com cotacao volta com as opcoes; CEP sem regiao volta com cidade vazia")
    void freteComCotacao() {
        when(geolocalizacaoService.buscarEndereco("99999999")).thenReturn(Optional.empty());
        temCarrinho();
        temItens(item(1L, 10L, 2));
        temPecas(peca(10L, LOJA_A, "100.00", 5, true));
        temLojas(loja(LOJA_A, true));
        when(freteService.cotar(LOJA_A, "99999999", new BigDecimal("200.00")))
                .thenReturn(Optional.of(List.of(new FreteService.OpcaoFrete("NORMAL", new BigDecimal("19.90"), 3))));

        FreteResponse frete = service.calcularFrete(ID_USUARIO, new FreteRequest("99999999"));

        assertThat(frete.regiao()).isEqualTo(new FreteResponse.Regiao("", ""));
        FreteResponse.LojaFrete loja = frete.lojas().get(0);
        assertThat(loja.freteACombinar()).isFalse();
        assertThat(loja.opcoes()).containsExactly(new FreteResponse.OpcaoFrete("NORMAL", new BigDecimal("19.90"), 3));
    }

    // ----- Apoio -----

    private void temCarrinho() {
        Carrinho carrinho = new Carrinho(ID_USUARIO);
        carrinho.setId(ID_CARRINHO);
        when(carrinhoRepository.findByIdUsuario(ID_USUARIO)).thenReturn(Optional.of(carrinho));
    }

    private void temItens(ItemCarrinho... itens) {
        when(itemRepository.findByIdCarrinhoOrderByIdAsc(ID_CARRINHO)).thenReturn(new ArrayList<>(Arrays.asList(itens)));
    }

    private void temPeca(PecaParaCompra peca) {
        when(pecaCompraService.buscar(peca.idPeca())).thenReturn(Optional.of(peca));
    }

    private void temPecas(PecaParaCompra... pecas) {
        Map<Long, PecaParaCompra> mapa = new LinkedHashMap<>();
        for (PecaParaCompra p : pecas) {
            mapa.put(p.idPeca(), p);
        }
        when(pecaCompraService.buscarVarias(anyCollection())).thenReturn(mapa);
    }

    private void temLojas(LojaResumoResponse... lojas) {
        Map<Long, LojaResumoResponse> mapa = new LinkedHashMap<>();
        for (LojaResumoResponse l : lojas) {
            mapa.put(l.idLoja(), l);
        }
        when(lojaService.resumos(anyCollection())).thenReturn(mapa);
    }

    private static ItemCarrinho item(Long id, Long idPeca, int quantidade) {
        ItemCarrinho item = new ItemCarrinho(ID_CARRINHO, idPeca, quantidade);
        item.setId(id);
        return item;
    }

    private static PecaParaCompra peca(Long id, Long idLoja, String preco, int estoque, boolean disponivel) {
        return new PecaParaCompra(id, idLoja, "Peça " + id, "OEM" + id, "SKU" + id, "Marca", null,
                new BigDecimal(preco), estoque, true, 0, disponivel);
    }

    private static LojaResumoResponse loja(Long id, boolean ativa) {
        return new LojaResumoResponse(id, "Loja " + id, true, true, false, ativa);
    }
}
