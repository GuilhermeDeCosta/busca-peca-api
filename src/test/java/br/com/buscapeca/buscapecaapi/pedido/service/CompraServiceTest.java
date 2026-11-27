package br.com.buscapeca.buscapecaapi.pedido.service;

import br.com.buscapeca.buscapecaapi.carrinho.service.CarrinhoService;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.ConflitoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaResumoResponse;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import br.com.buscapeca.buscapecaapi.peca.service.PecaCompraService;
import br.com.buscapeca.buscapecaapi.pedido.dto.CompraResponse;
import br.com.buscapeca.buscapecaapi.pedido.dto.CompraResponse.PedidoDaCompra;
import br.com.buscapeca.buscapecaapi.pedido.dto.FinalizarCompraRequest;
import br.com.buscapeca.buscapecaapi.pedido.dto.FinalizarCompraRequest.EscolhaLoja;
import br.com.buscapeca.buscapecaapi.pedido.entity.Compra;
import br.com.buscapeca.buscapecaapi.pedido.entity.ItemPedido;
import br.com.buscapeca.buscapecaapi.pedido.entity.Pedido;
import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;
import br.com.buscapeca.buscapecaapi.pedido.mapper.CompraMapper;
import br.com.buscapeca.buscapecaapi.pedido.repository.CompraRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.ItemPedidoRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.PedidoRepository;
import br.com.buscapeca.buscapecaapi.usuario.service.EnderecoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega.ENTREGA;
import static br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega.RETIRADA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Checkout (BP1-24, ADR-009): um pedido por loja, conferencias de estoque, preco e
 * modalidade, baixa de estoque e carrinho vazio no fim. Sem banco: tudo simulado. A trava
 * das linhas (SELECT ... FOR UPDATE) so da para conferir com o MySQL rodando.
 */
@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_ENDERECO = 3L;
    private static final Long ID_COMPRA = 900L;
    private static final Long LOJA_A = 7L;
    private static final Long LOJA_B = 8L;

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ItemPedidoRepository itemPedidoRepository;

    @Mock
    private CarrinhoService carrinhoService;

    @Mock
    private PecaCompraService pecaCompraService;

    @Mock
    private LojaService lojaService;

    @Mock
    private EnderecoService enderecoService;

    @Mock
    private GeradorCodigoRetirada geradorCodigo;

    private CompraService service;

    @BeforeEach
    void preparar() {
        service = new CompraService(compraRepository, pedidoRepository, itemPedidoRepository, carrinhoService,
                pecaCompraService, lojaService, enderecoService, geradorCodigo, new CompraMapper());
    }

    // ----- Caminho feliz -----

    @Test
    @DisplayName("Divide o carrinho em um pedido por loja, baixa o estoque e esvazia o carrinho")
    void finalizarUmPedidoPorLoja() {
        Map<Long, Integer> carrinho = carrinho(10L, 2, 11L, 1, 12L, 1);
        cenario(carrinho, peca(10L, LOJA_A, "100.00", 5, true), peca(11L, LOJA_B, "50.50", 5, true),
                peca(12L, LOJA_A, "20.00", 5, true));
        prepararGravacao("HP-3318", "AV-7421");

        CompraResponse compra = service.finalizar(ID_USUARIO, pedido(ID_ENDERECO,
                escolha(LOJA_A, RETIRADA, "  Bom dia  "), escolha(LOJA_B, ENTREGA, "")));

        assertThat(compra.idCompra()).isEqualTo(ID_COMPRA);
        assertThat(compra.total()).isEqualByComparingTo("270.50");
        assertThat(compra.idEndereco()).isEqualTo(ID_ENDERECO);
        assertThat(compra.pedidos()).hasSize(2);

        PedidoDaCompra daLojaA = compra.pedidos().get(0);
        assertThat(daLojaA.idLoja()).isEqualTo(LOJA_A);
        assertThat(daLojaA.codigo()).isEqualTo("BP-2081");
        assertThat(daLojaA.nomeLoja()).isEqualTo("Loja 7");
        assertThat(daLojaA.status()).isEqualTo(StatusPedido.AGUARDANDO_ACEITE);
        assertThat(daLojaA.codigoRetirada()).isEqualTo("HP-3318");
        assertThat(daLojaA.observacao()).isEqualTo("Bom dia");
        assertThat(daLojaA.subtotal()).isEqualByComparingTo("220.00");
        assertThat(daLojaA.itens()).extracting(CompraResponse.ItemDaCompra::titulo)
                .containsExactly("Peça 10", "Peça 12");

        PedidoDaCompra daLojaB = compra.pedidos().get(1);
        assertThat(daLojaB.modalidade()).isEqualTo(ENTREGA);
        assertThat(daLojaB.codigoRetirada()).as("na entrega o codigo nao aparece").isNull();
        assertThat(daLojaB.subtotal()).isEqualByComparingTo("50.50");

        verify(enderecoService).buscar(ID_USUARIO, ID_ENDERECO);
        verify(pecaCompraService).baixarEstoque(carrinho);
        verify(carrinhoService).esvaziar(ID_USUARIO);

        ArgumentCaptor<Pedido> pedidos = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository, times(2)).save(pedidos.capture());
        assertThat(pedidos.getAllValues().get(1).getCodigoRetirada())
                .as("todo pedido grava um codigo (coluna NOT NULL)").isEqualTo("AV-7421");
        assertThat(pedidos.getAllValues().get(1).getObservacao()).isNull();
    }

    @Test
    @DisplayName("So retirada: nao exige endereco e a compra fica sem endereco de entrega")
    void finalizarSoRetirada() {
        cenario(carrinho(10L, 1), peca(10L, LOJA_A, "100.00", 5, true));
        prepararGravacao("HP-3318");

        CompraResponse compra = service.finalizar(ID_USUARIO, pedido(null, escolha(LOJA_A, RETIRADA, null)));

        assertThat(compra.idEndereco()).isNull();
        verifyNoInteractions(enderecoService);
        ArgumentCaptor<Compra> salva = ArgumentCaptor.forClass(Compra.class);
        verify(compraRepository).save(salva.capture());
        assertThat(salva.getValue().getIdEnderecoEntrega()).isNull();
        assertThat(salva.getValue().getValorTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Codigo de retirada repetido e sorteado de novo")
    void codigoRetiradaRepetido() {
        cenario(carrinho(10L, 1), peca(10L, LOJA_A, "100.00", 5, true));
        prepararGravacao("HP-3318", "XY-1234");
        lenient().when(pedidoRepository.existsByCodigoRetirada("HP-3318")).thenReturn(true);

        CompraResponse compra = service.finalizar(ID_USUARIO, pedido(null, escolha(LOJA_A, RETIRADA, null)));

        assertThat(compra.pedidos().get(0).codigoRetirada()).isEqualTo("XY-1234");
    }

    // ----- Dados da requisicao (400) -----

    @Test
    @DisplayName("Entrega sem endereco da 400 no campo idEndereco, antes de travar qualquer peca")
    void entregaSemEndereco() {
        assertThatThrownBy(() -> service.finalizar(ID_USUARIO, pedido(null, escolha(LOJA_B, ENTREGA, null))))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("Escolha o endereço de entrega.")
                .extracting("campo").isEqualTo("idEndereco");
        verifyNoInteractions(carrinhoService, pecaCompraService, compraRepository);
    }

    @Test
    @DisplayName("Sem a ciencia do pagamento direto em alguma loja da 400")
    void semCiencia() {
        FinalizarCompraRequest req = new FinalizarCompraRequest(null,
                List.of(new EscolhaLoja(LOJA_A, RETIRADA, null, false)), null);

        assertThatThrownBy(() -> service.finalizar(ID_USUARIO, req))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("Confirme que leu as condições de cada loja.");
        verifyNoInteractions(carrinhoService, pecaCompraService);
    }

    @Test
    @DisplayName("Retirada numa loja sem balcao da 400")
    void retiradaSemBalcao() {
        cenario(carrinho(11L, 1), peca(11L, LOJA_B, "50.00", 5, true));

        assertThatThrownBy(() -> service.finalizar(ID_USUARIO, pedido(null, escolha(LOJA_B, RETIRADA, null))))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("A loja Loja 8 não oferece retirada no balcão.");
        verify(compraRepository, never()).save(any());
    }

    // ----- Algo mudou desde o carrinho (409) -----

    @Test
    @DisplayName("Carrinho vazio da 409 CARRINHO_VAZIO")
    void carrinhoVazio() {
        when(carrinhoService.itensParaCompra(ID_USUARIO)).thenReturn(Map.of());

        assertThatThrownBy(() -> service.finalizar(ID_USUARIO, pedido(null, escolha(LOJA_A, RETIRADA, null))))
                .isInstanceOf(ConflitoException.class)
                .extracting("codigo").isEqualTo("CARRINHO_VAZIO");
        verifyNoInteractions(pecaCompraService);
    }

    @Test
    @DisplayName("Estoque menor que o carrinho da 409 ESTOQUE_INSUFICIENTE e nada e gravado")
    void estoqueMudou() {
        cenario(carrinho(10L, 2), peca(10L, LOJA_A, "100.00", 1, true));

        assertThatThrownBy(() -> service.finalizar(ID_USUARIO, pedido(null, escolha(LOJA_A, RETIRADA, null))))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("O estoque de \"Peça 10\" mudou. Ajuste a quantidade no carrinho.")
                .extracting("codigo").isEqualTo("ESTOQUE_INSUFICIENTE");
        verify(compraRepository, never()).save(any());
        verify(pecaCompraService, never()).baixarEstoque(any());
        verify(carrinhoService, never()).esvaziar(any());
    }

    @Test
    @DisplayName("Anuncio pausado depois de ir para o carrinho da 409 ANUNCIO_INDISPONIVEL")
    void anuncioPausado() {
        cenario(carrinho(10L, 1), peca(10L, LOJA_A, "100.00", 5, false));

        assertThatThrownBy(() -> service.finalizar(ID_USUARIO, pedido(null, escolha(LOJA_A, RETIRADA, null))))
                .isInstanceOf(ConflitoException.class)
                .extracting("codigo").isEqualTo("ANUNCIO_INDISPONIVEL");
        verify(compraRepository, never()).save(any());
    }

    @Test
    @DisplayName("Loja do carrinho sem escolha na requisicao da 409 CARRINHO_ALTERADO")
    void carrinhoAlterado() {
        cenario(carrinho(10L, 1, 11L, 1), peca(10L, LOJA_A, "100.00", 5, true), peca(11L, LOJA_B, "50.00", 5, true));

        assertThatThrownBy(() -> service.finalizar(ID_USUARIO, pedido(null, escolha(LOJA_A, RETIRADA, null))))
                .isInstanceOf(ConflitoException.class)
                .extracting("codigo").isEqualTo("CARRINHO_ALTERADO");
        verify(compraRepository, never()).save(any());
    }

    @Test
    @DisplayName("Total diferente do que a tela mostrou da 409 PRECO_ALTERADO")
    void precoAlterado() {
        cenario(carrinho(10L, 1), peca(10L, LOJA_A, "89.90", 5, true));
        FinalizarCompraRequest req = new FinalizarCompraRequest(null,
                List.of(escolha(LOJA_A, RETIRADA, null)), new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.finalizar(ID_USUARIO, req))
                .isInstanceOf(ConflitoException.class)
                .extracting("codigo").isEqualTo("PRECO_ALTERADO");
        verify(compraRepository, never()).save(any());
    }

    // ----- Consultar -----

    @Test
    @DisplayName("Compra de outra pessoa da 404")
    void obterDeOutraPessoa() {
        when(compraRepository.findByIdAndIdComprador(ID_COMPRA, ID_USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obter(ID_USUARIO, ID_COMPRA))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Compra não encontrada.");
    }

    @Test
    @DisplayName("Detalhe traz os pedidos com itens, titulos e nome da loja")
    void obter() {
        Compra compra = new Compra();
        compra.setId(ID_COMPRA);
        compra.setIdComprador(ID_USUARIO);
        compra.setValorTotal(new BigDecimal("200.00"));
        compra.setDataCompra(LocalDateTime.of(2026, 11, 26, 21, 30));
        when(compraRepository.findByIdAndIdComprador(ID_COMPRA, ID_USUARIO)).thenReturn(Optional.of(compra));
        Pedido pedido = new Pedido();
        pedido.setId(2081L);
        pedido.setIdLoja(LOJA_A);
        pedido.setModalidadeEntrega(RETIRADA);
        pedido.setCodigoRetirada("HP-3318");
        pedido.setValorItens(new BigDecimal("200.00"));
        when(pedidoRepository.findByIdCompraOrderByIdAsc(ID_COMPRA)).thenReturn(List.of(pedido));
        ItemPedido item = new ItemPedido(2081L, 10L, 2, new BigDecimal("100.00"));
        when(itemPedidoRepository.findByIdPedidoInOrderByIdAsc(List.of(2081L))).thenReturn(List.of(item));
        when(pecaCompraService.buscarVarias(anyCollection()))
                .thenReturn(Map.of(10L, peca(10L, LOJA_A, "120.00", 5, true)));
        when(lojaService.resumos(anyCollection())).thenReturn(Map.of(LOJA_A, loja(LOJA_A, true, true)));

        CompraResponse resposta = service.obter(ID_USUARIO, ID_COMPRA);

        PedidoDaCompra p = resposta.pedidos().get(0);
        assertThat(p.codigo()).isEqualTo("BP-2081");
        assertThat(p.nomeLoja()).isEqualTo("Loja 7");
        assertThat(p.codigoRetirada()).isEqualTo("HP-3318");
        assertThat(p.itens().get(0).titulo()).isEqualTo("Peça 10");
        assertThat(p.itens().get(0).precoUnitario())
                .as("o preco e o do momento da compra, nao o de agora").isEqualByComparingTo("100.00");
    }

    // ----- Apoio -----

    /** Carrinho, pecas travadas e lojas: A com balcao e entrega, B so com entrega. */
    private void cenario(Map<Long, Integer> carrinho, PecaParaCompra... pecas) {
        when(carrinhoService.itensParaCompra(ID_USUARIO)).thenReturn(carrinho);
        Map<Long, PecaParaCompra> mapa = new LinkedHashMap<>();
        for (PecaParaCompra p : pecas) {
            mapa.put(p.idPeca(), p);
        }
        when(pecaCompraService.travarParaCompra(anyCollection())).thenReturn(mapa);
        Map<Long, LojaResumoResponse> lojas = new LinkedHashMap<>();
        lojas.put(LOJA_A, loja(LOJA_A, true, true));
        lojas.put(LOJA_B, loja(LOJA_B, false, true));
        when(lojaService.resumos(anyCollection())).thenReturn(lojas);
    }

    /** Ids gerados pelo banco e codigos de retirada sorteados, na ordem. */
    private void prepararGravacao(String... codigos) {
        lenient().when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> {
            Compra c = inv.getArgument(0);
            c.setId(ID_COMPRA);
            return c;
        });
        AtomicLong proximoPedido = new AtomicLong(2081);
        lenient().when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(proximoPedido.getAndIncrement());
            return p;
        });
        lenient().when(itemPedidoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(pedidoRepository.existsByCodigoRetirada(anyString())).thenReturn(false);
        if (codigos.length == 1) {
            when(geradorCodigo.gerar()).thenReturn(codigos[0]);
        } else {
            String[] resto = Arrays.copyOfRange(codigos, 1, codigos.length);
            when(geradorCodigo.gerar()).thenReturn(codigos[0], resto);
        }
    }

    private static Map<Long, Integer> carrinho(Object... pares) {
        Map<Long, Integer> mapa = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            mapa.put((Long) pares[i], (Integer) pares[i + 1]);
        }
        return mapa;
    }

    private static FinalizarCompraRequest pedido(Long idEndereco, EscolhaLoja... escolhas) {
        return new FinalizarCompraRequest(idEndereco, List.of(escolhas), null);
    }

    private static EscolhaLoja escolha(Long idLoja, ModalidadeEntrega modalidade, String observacao) {
        return new EscolhaLoja(idLoja, modalidade, observacao, true);
    }

    private static PecaParaCompra peca(Long id, Long idLoja, String preco, int estoque, boolean disponivel) {
        return new PecaParaCompra(id, idLoja, "Peça " + id, "OEM" + id, "SKU" + id, "Marca", null,
                new BigDecimal(preco), estoque, true, 0, disponivel);
    }

    private static LojaResumoResponse loja(Long id, boolean retiradaBalcao, boolean entregaCombinar) {
        return new LojaResumoResponse(id, "Loja " + id, retiradaBalcao, entregaCombinar, false, true);
    }
}
