package br.com.buscapeca.buscapecaapi.pedido.service;

import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaResumoResponse;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import br.com.buscapeca.buscapecaapi.peca.service.PecaCompraService;
import br.com.buscapeca.buscapecaapi.pedido.dto.MinhasComprasResponse;
import br.com.buscapeca.buscapecaapi.pedido.dto.MinhasComprasResponse.CompraDoHistorico;
import br.com.buscapeca.buscapecaapi.pedido.dto.MinhasComprasResponse.ItemDoHistorico;
import br.com.buscapeca.buscapecaapi.pedido.dto.MinhasComprasResponse.PedidoDoHistorico;
import br.com.buscapeca.buscapecaapi.pedido.entity.Compra;
import br.com.buscapeca.buscapecaapi.pedido.entity.ItemPedido;
import br.com.buscapeca.buscapecaapi.pedido.entity.Pedido;
import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;
import br.com.buscapeca.buscapecaapi.pedido.mapper.CompraMapper;
import br.com.buscapeca.buscapecaapi.pedido.mapper.PedidoMapper;
import br.com.buscapeca.buscapecaapi.pedido.repository.CompraRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.ItemPedidoRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega.ENTREGA;
import static br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega.RETIRADA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Meus pedidos (GET /compras; BP1-20, BP1-27): compras do comprador com o pedido de cada
 * loja, filtros por codigo, loja, peca, status e periodo, pagina comecando em 0 e o resumo
 * dos cartoes sem filtro. Sem banco: tudo simulado.
 */
@ExtendWith(MockitoExtension.class)
class MinhasComprasServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long LOJA_AVENIDA = 7L;
    private static final Long LOJA_NORTE = 8L;

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ItemPedidoRepository itemPedidoRepository;

    @Mock
    private PecaCompraService pecaCompraService;

    @Mock
    private LojaService lojaService;

    private MinhasComprasService service;

    @BeforeEach
    void preparar() {
        service = new MinhasComprasService(compraRepository, pedidoRepository, itemPedidoRepository,
                pecaCompraService, lojaService, new CompraMapper(), new PedidoMapper());
    }

    // ----- Lista -----

    @Test
    @DisplayName("Lista as compras da mais recente, com o pedido de cada loja e o resumo dos itens")
    void listarTudo() {
        historico();

        MinhasComprasResponse resposta = service.listar(ID_USUARIO, null, null, null, null, null, null);

        assertThat(resposta.itens()).extracting(CompraDoHistorico::idCompra).containsExactly(1041L, 1038L);
        assertThat(resposta.pagina()).isZero();
        assertThat(resposta.totalRegistros()).isEqualTo(2);
        assertThat(resposta.totalPaginas()).isEqualTo(1);

        CompraDoHistorico compra = resposta.itens().get(0);
        assertThat(compra.total()).isEqualByComparingTo("549.70");
        assertThat(compra.pedidos()).extracting(PedidoDoHistorico::codigo).containsExactly("BP-1042", "BP-1043");

        PedidoDoHistorico retirada = compra.pedidos().get(0);
        assertThat(retirada.nomeLoja()).isEqualTo("Autopeças Avenida");
        assertThat(retirada.status()).isEqualTo(StatusPedido.AGUARDANDO_ACEITE);
        assertThat(retirada.modalidade()).isEqualTo(RETIRADA);
        assertThat(retirada.codigoRetirada()).isEqualTo("AV-7421");
        assertThat(retirada.subtotal()).isEqualByComparingTo("489.90");
        assertThat(retirada.resumoItens()).isEqualTo("1 Farol direito Chevrolet Onix");
        assertThat(retirada.itens()).containsExactly(new ItemDoHistorico("Farol direito Chevrolet Onix", 1));

        PedidoDoHistorico entrega = compra.pedidos().get(1);
        assertThat(entrega.codigoRetirada()).as("na entrega o codigo nao aparece").isNull();
        assertThat(entrega.resumoItens()).isEqualTo("2 Filtro de óleo Tecfil e mais 1 item");
    }

    @Test
    @DisplayName("O resumo conta todos os pedidos do comprador, mesmo com filtro")
    void resumoSemFiltro() {
        historico();

        MinhasComprasResponse resposta = service.listar(ID_USUARIO, null, "CONCLUIDO", null, null, null, null);

        assertThat(resposta.itens()).isEmpty();
        assertThat(resposta.resumo().atencao()).as("PRONTO_RETIRADA + EM_ENTREGA").isEqualTo(3);
        assertThat(resposta.resumo().andamento()).as("AGUARDANDO_ACEITE + ACEITO + EM_PREPARACAO").isEqualTo(2);
        assertThat(resposta.resumo().retirada()).isEqualTo(1);
    }

    @Test
    @DisplayName("Pagina comeca em 0 e conta as compras, nao os pedidos")
    void pagina() {
        historico();

        MinhasComprasResponse resposta = service.listar(ID_USUARIO, null, null, null, null, 1, 1);

        assertThat(resposta.itens()).extracting(CompraDoHistorico::idCompra).containsExactly(1038L);
        assertThat(resposta.pagina()).isEqualTo(1);
        assertThat(resposta.tamanho()).isEqualTo(1);
        assertThat(resposta.totalRegistros()).isEqualTo(2);
        assertThat(resposta.totalPaginas()).isEqualTo(2);
    }

    @Test
    @DisplayName("Pagina depois da ultima volta vazia, sem erro")
    void paginaDepoisDaUltima() {
        historico();

        MinhasComprasResponse resposta = service.listar(ID_USUARIO, null, null, null, null, 5, 6);

        assertThat(resposta.itens()).isEmpty();
        assertThat(resposta.totalRegistros()).isEqualTo(2);
    }

    @Test
    @DisplayName("Sem compras: lista vazia e nao consulta pedidos, pecas nem lojas")
    void semCompras() {
        when(compraRepository.findByIdCompradorAndDataCompraGreaterThanEqualAndDataCompraLessThanOrderByDataCompraDescIdDesc(
                eq(ID_USUARIO), any(), any())).thenReturn(List.of());
        when(pedidoRepository.contarPorStatusDoComprador(ID_USUARIO)).thenReturn(List.of());

        MinhasComprasResponse resposta = service.listar(ID_USUARIO, null, null, null, null, null, null);

        assertThat(resposta.itens()).isEmpty();
        assertThat(resposta.totalPaginas()).isZero();
        assertThat(resposta.resumo()).isEqualTo(new MinhasComprasResponse.Resumo(0, 0, 0));
        verifyNoInteractions(itemPedidoRepository, pecaCompraService, lojaService);
    }

    // ----- Filtros -----

    @Test
    @DisplayName("Status filtra os pedidos; a compra fica so com os que passaram")
    void filtroStatus() {
        historico();

        MinhasComprasResponse resposta = service.listar(ID_USUARIO, null, "aceito", null, null, null, null);

        assertThat(resposta.itens()).hasSize(1);
        CompraDoHistorico compra = resposta.itens().get(0);
        assertThat(compra.pedidos()).extracting(PedidoDoHistorico::codigo).containsExactly("BP-1043");
        assertThat(compra.total()).as("o total continua sendo o da compra inteira").isEqualByComparingTo("549.70");
    }

    @Test
    @DisplayName("Busca pelo numero do pedido, com ou sem BP-")
    void buscaPeloCodigo() {
        historico();

        assertThat(codigos(service.listar(ID_USUARIO, "1039", null, null, null, null, null)))
                .containsExactly("BP-1039");
        assertThat(codigos(service.listar(ID_USUARIO, "bp-1043", null, null, null, null, null)))
                .containsExactly("BP-1043");
    }

    @Test
    @DisplayName("Busca pela loja ou pela peca, sem acento e sem maiusculas")
    void buscaPorTexto() {
        historico();

        assertThat(codigos(service.listar(ID_USUARIO, "autopecas avenida", null, null, null, null, null)))
                .containsExactly("BP-1042", "BP-1039");
        assertThat(codigos(service.listar(ID_USUARIO, "OLEO", null, null, null, null, null)))
                .as("acha pelo titulo de um item que nao e o primeiro")
                .containsExactly("BP-1043");
        assertThat(service.listar(ID_USUARIO, "amortecedor", null, null, null, null, null).itens()).isEmpty();
    }

    @Test
    @DisplayName("Periodo vai do inicio de 'de' ate o fim do dia de 'ate'")
    void periodo() {
        historico();

        service.listar(ID_USUARIO, null, null, "2026-10-14", "2026-10-16", null, null);

        verify(compraRepository).findByIdCompradorAndDataCompraGreaterThanEqualAndDataCompraLessThanOrderByDataCompraDescIdDesc(
                ID_USUARIO, LocalDateTime.of(2026, 10, 14, 0, 0), LocalDateTime.of(2026, 10, 17, 0, 0));
    }

    @Test
    @DisplayName("Status que nao existe da 400")
    void statusInvalido() {
        assertThatThrownBy(() -> service.listar(ID_USUARIO, null, "PERDIDO", null, null, null, null))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("Status de pedido inválido.");
    }

    @Test
    @DisplayName("Data fora do formato AAAA-MM-DD da 400 no campo certo")
    void dataInvalida() {
        assertThatThrownBy(() -> service.listar(ID_USUARIO, null, null, null, "16/10/2026", null, null))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("Data inválida. Use o formato AAAA-MM-DD.")
                .extracting(e -> ((DadosInvalidosException) e).getCampo()).isEqualTo("ate");
    }

    @Test
    @DisplayName("'de' depois de 'ate' da 400 sem consultar o banco")
    void periodoInvertido() {
        assertThatThrownBy(() -> service.listar(ID_USUARIO, null, null, "2026-10-20", "2026-10-10", null, null))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("A data final deve ser igual ou depois da inicial.");
        verifyNoInteractions(compraRepository, pedidoRepository);
    }

    // ----- Apoio -----

    /**
     * Compra 1041 (16/10): BP-1042 retirada na Avenida, aguardando aceite; BP-1043 entrega na
     * Norte, aceito, com dois itens. Compra 1038 (14/10): BP-1039 retirada na Avenida, pronto.
     * O resumo vem do banco: 1 pronto, 2 em entrega, 1 aguardando, 1 aceito, 4 concluidos.
     */
    private void historico() {
        Compra recente = compra(1041L, "549.70", LocalDateTime.of(2026, 10, 16, 9, 42));
        Compra antiga = compra(1038L, "219.90", LocalDateTime.of(2026, 10, 14, 15, 20));
        lenient().when(compraRepository
                        .findByIdCompradorAndDataCompraGreaterThanEqualAndDataCompraLessThanOrderByDataCompraDescIdDesc(
                                eq(ID_USUARIO), any(), any()))
                .thenReturn(List.of(recente, antiga));
        lenient().when(pedidoRepository.findByIdCompraInOrderByIdAsc(List.of(1041L, 1038L))).thenReturn(List.of(
                pedido(1039L, 1038L, LOJA_AVENIDA, RETIRADA, StatusPedido.PRONTO_RETIRADA, "RS-6190", "219.90"),
                pedido(1042L, 1041L, LOJA_AVENIDA, RETIRADA, StatusPedido.AGUARDANDO_ACEITE, "AV-7421", "489.90"),
                pedido(1043L, 1041L, LOJA_NORTE, ENTREGA, StatusPedido.ACEITO, "MN-1111", "59.80")));
        lenient().when(itemPedidoRepository.findByIdPedidoInOrderByIdAsc(List.of(1039L, 1042L, 1043L))).thenReturn(List.of(
                new ItemPedido(1039L, 12L, 1, new BigDecimal("219.90")),
                new ItemPedido(1042L, 10L, 1, new BigDecimal("489.90")),
                new ItemPedido(1043L, 11L, 2, new BigDecimal("24.90")),
                new ItemPedido(1043L, 13L, 1, new BigDecimal("10.00"))));
        lenient().when(pecaCompraService.buscarVarias(anyCollection())).thenReturn(Map.of(
                10L, peca(10L, LOJA_AVENIDA, "Farol direito Chevrolet Onix"),
                11L, peca(11L, LOJA_NORTE, "Filtro de óleo Tecfil"),
                12L, peca(12L, LOJA_AVENIDA, "Retrovisor elétrico esquerdo HB20"),
                13L, peca(13L, LOJA_NORTE, "Óleo 5W30 1L")));
        lenient().when(lojaService.resumos(anyCollection())).thenReturn(Map.of(
                LOJA_AVENIDA, loja(LOJA_AVENIDA, "Autopeças Avenida"),
                LOJA_NORTE, loja(LOJA_NORTE, "Motor Norte Peças")));
        List<Object[]> contagem = new ArrayList<>();
        contagem.add(new Object[]{StatusPedido.PRONTO_RETIRADA, 1L});
        contagem.add(new Object[]{StatusPedido.EM_ENTREGA, 2L});
        contagem.add(new Object[]{StatusPedido.AGUARDANDO_ACEITE, 1L});
        contagem.add(new Object[]{StatusPedido.ACEITO, 1L});
        contagem.add(new Object[]{StatusPedido.CONCLUIDO, 4L});
        lenient().when(pedidoRepository.contarPorStatusDoComprador(ID_USUARIO)).thenReturn(contagem);
    }

    private static List<String> codigos(MinhasComprasResponse resposta) {
        return resposta.itens().stream().flatMap(c -> c.pedidos().stream()).map(PedidoDoHistorico::codigo).toList();
    }

    private static Compra compra(Long id, String total, LocalDateTime data) {
        Compra c = new Compra();
        c.setId(id);
        c.setIdComprador(ID_USUARIO);
        c.setValorTotal(new BigDecimal(total));
        c.setDataCompra(data);
        return c;
    }

    private static Pedido pedido(Long id, Long idCompra, Long idLoja, ModalidadeEntrega modalidade,
                                 StatusPedido status, String codigoRetirada, String valorItens) {
        Pedido p = new Pedido();
        p.setId(id);
        p.setIdCompra(idCompra);
        p.setIdLoja(idLoja);
        p.setModalidadeEntrega(modalidade);
        p.setStatus(status);
        p.setCodigoRetirada(codigoRetirada);
        p.setValorItens(new BigDecimal(valorItens));
        return p;
    }

    private static PecaParaCompra peca(Long id, Long idLoja, String titulo) {
        return new PecaParaCompra(id, idLoja, titulo, "OEM" + id, "SKU" + id, "Marca", null,
                new BigDecimal("10.00"), 5, true, 0, true);
    }

    private static LojaResumoResponse loja(Long id, String nome) {
        return new LojaResumoResponse(id, nome, true, true, false, true);
    }
}
