package br.com.buscapeca.buscapecaapi.pedido.service;

import br.com.buscapeca.buscapecaapi.compartilhado.excecao.AcessoNegadoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.ConflitoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.NaoProcessavelException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.compartilhado.paginacao.PaginaResposta;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaResumoResponse;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import br.com.buscapeca.buscapecaapi.peca.service.PecaCompraService;
import br.com.buscapeca.buscapecaapi.pedido.dto.ConfirmarRetiradaRequest;
import br.com.buscapeca.buscapecaapi.pedido.dto.EnviarPedidoRequest;
import br.com.buscapeca.buscapecaapi.pedido.dto.MotivoRequest;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoDetalheResponse;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoRecebidoResponse;
import br.com.buscapeca.buscapecaapi.pedido.entity.Compra;
import br.com.buscapeca.buscapecaapi.pedido.entity.Entrega;
import br.com.buscapeca.buscapecaapi.pedido.entity.ItemPedido;
import br.com.buscapeca.buscapecaapi.pedido.entity.Pedido;
import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEnvio;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;
import br.com.buscapeca.buscapecaapi.pedido.mapper.PedidoMapper;
import br.com.buscapeca.buscapecaapi.pedido.repository.CompraRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.EntregaRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.ItemPedidoRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.PedidoConsultaRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.PedidoRepository;
import br.com.buscapeca.buscapecaapi.usuario.dto.EnderecoResponse;
import br.com.buscapeca.buscapecaapi.usuario.dto.UsuarioResponse;
import br.com.buscapeca.buscapecaapi.usuario.enums.StatusUsuario;
import br.com.buscapeca.buscapecaapi.usuario.enums.TipoPessoa;
import br.com.buscapeca.buscapecaapi.usuario.service.EnderecoService;
import br.com.buscapeca.buscapecaapi.usuario.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Ciclo do pedido (BP1-26, 27, 29, 30, 33): quem pode cada mudanca, a ordem dos status,
 * a devolucao de estoque e o codigo de retirada. Sem banco: tudo simulado, com o
 * PedidoMapper de verdade.
 */
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final Long COMPRADOR = 1L;
    private static final Long VENDEDOR = 2L;
    private static final Long OUTRA_PESSOA = 99L;
    private static final Long LOJA = 7L;
    private static final Long ID_COMPRA = 900L;
    private static final Long ID_PEDIDO = 2081L;
    private static final Long ID_ENDERECO = 3L;

    @Mock
    private PedidoConsultaRepository consultaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ItemPedidoRepository itemRepository;

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private EntregaRepository entregaRepository;

    @Mock
    private PecaCompraService pecaCompraService;

    @Mock
    private LojaService lojaService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private EnderecoService enderecoService;

    private PedidoService service;

    @BeforeEach
    void preparar() {
        service = new PedidoService(consultaRepository, pedidoRepository, itemRepository, compraRepository,
                entregaRepository, pecaCompraService, lojaService, usuarioService, enderecoService, new PedidoMapper());
    }

    // ----- Loja -----

    @Test
    @DisplayName("A loja aceita; a resposta e a visao da loja, sem o codigo de retirada")
    void aceitar() {
        Pedido pedido = temPedido(ModalidadeEntrega.RETIRADA, StatusPedido.AGUARDANDO_ACEITE);

        PedidoDetalheResponse resposta = service.aceitar(VENDEDOR, ID_PEDIDO);

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.ACEITO);
        verify(pedidoRepository).saveAndFlush(pedido);
        assertThat(resposta.visao()).isEqualTo("VENDEDOR");
        assertThat(resposta.codigo()).isEqualTo("BP-2081");
        assertThat(resposta.codigoRetirada()).as("a loja nunca recebe o codigo").isNull();
        assertThat(resposta.comprador().nome()).isEqualTo("Oficina Rumo Certo");
        assertThat(resposta.comprador().nomeContato()).isEqualTo("Carlos Pereira");
        assertThat(resposta.comprador().cidade()).isEqualTo("Campinas");
        assertThat(resposta.itens().get(0).estoqueAtual()).isEqualTo(4);
        assertThat(resposta.historico()).extracting(PedidoDetalheResponse.Passo::status)
                .containsExactly(StatusPedido.AGUARDANDO_ACEITE, StatusPedido.ACEITO);
    }

    @Test
    @DisplayName("O comprador nao pode aceitar o proprio pedido (403)")
    void aceitarComoComprador() {
        temPedido(ModalidadeEntrega.RETIRADA, StatusPedido.AGUARDANDO_ACEITE);

        assertThatThrownBy(() -> service.aceitar(COMPRADOR, ID_PEDIDO))
                .isInstanceOf(AcessoNegadoException.class);
        verify(pedidoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Aceitar de novo da 409 STATUS_INVALIDO")
    void aceitarJaAceito() {
        temPedido(ModalidadeEntrega.RETIRADA, StatusPedido.ACEITO);

        assertThatThrownBy(() -> service.aceitar(VENDEDOR, ID_PEDIDO))
                .isInstanceOf(ConflitoException.class)
                .extracting("codigo").isEqualTo("STATUS_INVALIDO");
    }

    @Test
    @DisplayName("Quem nao e comprador nem a loja recebe 404")
    void pedidoDeOutraPessoa() {
        temPedido(ModalidadeEntrega.RETIRADA, StatusPedido.AGUARDANDO_ACEITE);
        when(lojaService.idDaLoja(OUTRA_PESSOA)).thenReturn(Optional.of(55L));

        assertThatThrownBy(() -> service.aceitar(OUTRA_PESSOA, ID_PEDIDO))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Pedido não encontrado.");
    }

    @Test
    @DisplayName("Recusar grava o motivo e devolve o estoque")
    void recusar() {
        Pedido pedido = temPedido(ModalidadeEntrega.RETIRADA, StatusPedido.AGUARDANDO_ACEITE);

        service.recusar(VENDEDOR, ID_PEDIDO, new MotivoRequest("  Peça sem estoque físico ", "Desculpe"));

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.RECUSADO);
        assertThat(pedido.getMotivoRecusa()).isEqualTo("Peça sem estoque físico");
        verify(pecaCompraService).devolverEstoque(Map.of(10L, 2));
    }

    @Test
    @DisplayName("Confirmar retirada aceita o codigo com ou sem hifen e conclui")
    void confirmarRetirada() {
        Pedido pedido = temPedido(ModalidadeEntrega.RETIRADA, StatusPedido.PRONTO_RETIRADA);

        service.confirmarRetirada(VENDEDOR, ID_PEDIDO, new ConfirmarRetiradaRequest("hp 3318"));

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CONCLUIDO);
    }

    @Test
    @DisplayName("Codigo errado da 422 CODIGO_RETIRADA_INVALIDO e o pedido nao muda")
    void confirmarRetiradaCodigoErrado() {
        Pedido pedido = temPedido(ModalidadeEntrega.RETIRADA, StatusPedido.PRONTO_RETIRADA);

        assertThatThrownBy(() -> service.confirmarRetirada(VENDEDOR, ID_PEDIDO, new ConfirmarRetiradaRequest("HP-0000")))
                .isInstanceOf(NaoProcessavelException.class)
                .hasMessage("Código de retirada não confere.")
                .extracting("codigo").isEqualTo("CODIGO_RETIRADA_INVALIDO");
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PRONTO_RETIRADA);
        verify(pedidoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Pronto para retirada em pedido de entrega da 409 MODALIDADE_INVALIDA")
    void prontoRetiradaEmEntrega() {
        temPedido(ModalidadeEntrega.ENTREGA, StatusPedido.ACEITO);

        assertThatThrownBy(() -> service.prontoRetirada(VENDEDOR, ID_PEDIDO))
                .isInstanceOf(ConflitoException.class)
                .extracting("codigo").isEqualTo("MODALIDADE_INVALIDA");
    }

    @Test
    @DisplayName("Preparacao so a partir de ACEITO")
    void emPreparacao() {
        Pedido pedido = temPedido(ModalidadeEntrega.ENTREGA, StatusPedido.ACEITO);

        service.emPreparacao(VENDEDOR, ID_PEDIDO);

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.EM_PREPARACAO);
    }

    @Test
    @DisplayName("Enviar grava a entrega em transito e a resposta traz o endereco em uma linha")
    void enviar() {
        Pedido pedido = temPedido(ModalidadeEntrega.ENTREGA, StatusPedido.EM_PREPARACAO);

        PedidoDetalheResponse resposta = service.enviar(VENDEDOR, ID_PEDIDO,
                new EnviarPedidoRequest(ModalidadeEnvio.TRANSPORTADORA, "Jadlog", "JD123"));

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.EM_ENTREGA);
        ArgumentCaptor<Entrega> entrega = ArgumentCaptor.forClass(Entrega.class);
        verify(entregaRepository).save(entrega.capture());
        assertThat(entrega.getValue().getStatusEntrega()).isEqualTo(StatusEntrega.EM_TRANSITO);
        assertThat(entrega.getValue().getTransportadora()).isEqualTo("Jadlog");
        assertThat(resposta.enderecoEntrega())
                .isEqualTo("Av. dos Imigrantes, 455 • Centro • Valinhos/SP • CEP 13270-000");
        assertThat(resposta.comprador().cidade()).as("cidade do endereco de entrega").isEqualTo("Valinhos");
    }

    @Test
    @DisplayName("Transportadora sem nome da 400, antes de travar o pedido")
    void enviarSemTransportadora() {
        assertThatThrownBy(() -> service.enviar(VENDEDOR, ID_PEDIDO,
                new EnviarPedidoRequest(ModalidadeEnvio.TRANSPORTADORA, " ", null)))
                .isInstanceOf(DadosInvalidosException.class);
        verifyNoInteractions(pedidoRepository);
    }

    // ----- Comprador -----

    @Test
    @DisplayName("O comprador cancela antes do aceite; o estoque volta e ele ve o codigo de retirada")
    void cancelar() {
        Pedido pedido = temPedido(ModalidadeEntrega.RETIRADA, StatusPedido.AGUARDANDO_ACEITE);

        PedidoDetalheResponse resposta = service.cancelar(COMPRADOR, ID_PEDIDO, new MotivoRequest("Comprei em outro lugar", ""));

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(pedido.getMotivoCancelamento()).isEqualTo("Comprei em outro lugar");
        verify(pecaCompraService).devolverEstoque(Map.of(10L, 2));
        assertThat(resposta.visao()).isEqualTo("COMPRADOR");
        assertThat(resposta.codigoRetirada()).isEqualTo("HP-3318");
    }

    @Test
    @DisplayName("Depois do aceite o comprador nao cancela (409 com a mensagem da tela)")
    void cancelarDepoisDoAceite() {
        temPedido(ModalidadeEntrega.RETIRADA, StatusPedido.ACEITO);

        assertThatThrownBy(() -> service.cancelar(COMPRADOR, ID_PEDIDO, new MotivoRequest("Desisti", null)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("A loja já aceitou este pedido. Converse com a loja para combinar o cancelamento.");
        verify(pecaCompraService, never()).devolverEstoque(any());
    }

    @Test
    @DisplayName("A loja nao cancela pelo comprador (403)")
    void cancelarComoLoja() {
        temPedido(ModalidadeEntrega.RETIRADA, StatusPedido.AGUARDANDO_ACEITE);

        assertThatThrownBy(() -> service.cancelar(VENDEDOR, ID_PEDIDO, new MotivoRequest("x", null)))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("O comprador confirma que recebeu: conclui e marca a entrega como entregue")
    void concluirComoComprador() {
        Pedido pedido = temPedido(ModalidadeEntrega.ENTREGA, StatusPedido.EM_ENTREGA);
        Entrega entrega = new Entrega();
        entrega.setIdPedido(ID_PEDIDO);
        entrega.setModalidade(ModalidadeEnvio.MOTOBOY);
        entrega.setStatusEntrega(StatusEntrega.EM_TRANSITO);
        when(entregaRepository.findByIdPedido(ID_PEDIDO)).thenReturn(Optional.of(entrega));

        PedidoDetalheResponse resposta = service.concluir(COMPRADOR, ID_PEDIDO);

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CONCLUIDO);
        assertThat(entrega.getStatusEntrega()).isEqualTo(StatusEntrega.ENTREGUE);
        assertThat(resposta.entrega().modalidade()).isEqualTo(ModalidadeEnvio.MOTOBOY);
    }

    // ----- Lista da loja -----

    @Test
    @DisplayName("Pedidos recebidos: pagina comeca em 1, com comprador e resumo dos itens")
    void listarRecebidos() {
        when(lojaService.idDaLoja(VENDEDOR)).thenReturn(Optional.of(LOJA));
        Pedido pedido = pedido(ModalidadeEntrega.RETIRADA, StatusPedido.AGUARDANDO_ACEITE);
        when(pedidoRepository.findByIdLojaAndStatus(eq(LOJA), eq(StatusPedido.AGUARDANDO_ACEITE), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(pedido), inv.getArgument(2), 1));
        when(itemRepository.findByIdPedidoInOrderByIdAsc(List.of(ID_PEDIDO))).thenReturn(List.of(
                new ItemPedido(ID_PEDIDO, 10L, 2, new BigDecimal("100.00")),
                new ItemPedido(ID_PEDIDO, 11L, 1, new BigDecimal("50.00"))));
        when(pecaCompraService.buscarVarias(anyCollection())).thenReturn(Map.of(
                10L, peca(10L, "filtros de ar"), 11L, peca(11L, "vela")));
        when(compraRepository.findAllById(anyCollection())).thenReturn(List.of(compra(null)));
        when(usuarioService.buscarPerfil(COMPRADOR)).thenReturn(comprador());

        PaginaResposta<PedidoRecebidoResponse> pagina = service.listarRecebidos(VENDEDOR, "aguardando_aceite", 1, 3);

        assertThat(pagina.pagina()).isEqualTo(1);
        PedidoRecebidoResponse linha = pagina.itens().get(0);
        assertThat(linha.codigo()).isEqualTo("BP-2081");
        assertThat(linha.comprador()).isEqualTo("Oficina Rumo Certo");
        assertThat(linha.resumoItens()).isEqualTo("2 filtros de ar e mais 1 item");
        ArgumentCaptor<Pageable> pedido0 = ArgumentCaptor.forClass(Pageable.class);
        verify(pedidoRepository).findByIdLojaAndStatus(eq(LOJA), eq(StatusPedido.AGUARDANDO_ACEITE), pedido0.capture());
        assertThat(pedido0.getValue().getPageNumber()).isZero();
        assertThat(pedido0.getValue().getPageSize()).isEqualTo(3);
    }

    @Test
    @DisplayName("Status invalido no filtro da 400; conta sem loja da 403")
    void listarRecebidosErros() {
        when(lojaService.idDaLoja(VENDEDOR)).thenReturn(Optional.of(LOJA));
        assertThatThrownBy(() -> service.listarRecebidos(VENDEDOR, "XYZ", 1, 3))
                .isInstanceOf(DadosInvalidosException.class);

        when(lojaService.idDaLoja(COMPRADOR)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.listarRecebidos(COMPRADOR, null, 1, 3))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("Codigo normalizado: sem hifen, espacos e em maiusculas")
    void normalizarCodigo() {
        assertThat(PedidoService.normalizarCodigo(" hp-33 18 ")).isEqualTo("HP3318");
        assertThat(PedidoService.normalizarCodigo(null)).isEmpty();
    }

    // ----- Apoio -----

    /**
     * Pedido travado e tudo que a montagem do detalhe le. Os stubs da montagem sao lenient:
     * os testes que falham antes de montar nao os usam.
     */
    private Pedido temPedido(ModalidadeEntrega modalidade, StatusPedido status) {
        Pedido pedido = pedido(modalidade, status);
        when(pedidoRepository.travar(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        Long endereco = modalidade == ModalidadeEntrega.ENTREGA ? ID_ENDERECO : null;
        when(compraRepository.findById(ID_COMPRA)).thenReturn(Optional.of(compra(endereco)));
        lenient().when(lojaService.idDaLoja(VENDEDOR)).thenReturn(Optional.of(LOJA));
        lenient().when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setAtualizadoEm(LocalDateTime.of(2026, 11, 27, 10, 0));
            return p;
        });
        lenient().when(itemRepository.findByIdPedidoOrderByIdAsc(ID_PEDIDO))
                .thenReturn(List.of(new ItemPedido(ID_PEDIDO, 10L, 2, new BigDecimal("100.00"))));
        lenient().when(pecaCompraService.buscarVarias(anyCollection())).thenReturn(Map.of(10L, peca(10L, "Alternador Bosch")));
        lenient().when(lojaService.resumos(anyCollection()))
                .thenReturn(Map.of(LOJA, new LojaResumoResponse(LOJA, "Auto Peças Centro", true, true, false, true)));
        lenient().when(usuarioService.buscarPerfil(COMPRADOR)).thenReturn(comprador());
        lenient().when(enderecoService.listar(COMPRADOR)).thenReturn(List.of(endereco(10L, "Campinas", true)));
        lenient().when(enderecoService.buscarPorId(ID_ENDERECO))
                .thenReturn(new EnderecoResponse(ID_ENDERECO, "Oficina", "13270000", "Av. dos Imigrantes", "455", null,
                        "Centro", "Valinhos", "SP", null, null, false));
        lenient().when(entregaRepository.findByIdPedido(anyLong())).thenReturn(Optional.empty());
        return pedido;
    }

    private static Pedido pedido(ModalidadeEntrega modalidade, StatusPedido status) {
        Pedido p = new Pedido();
        p.setId(ID_PEDIDO);
        p.setIdCompra(ID_COMPRA);
        p.setIdLoja(LOJA);
        p.setModalidadeEntrega(modalidade);
        p.setStatus(status);
        p.setCodigoRetirada("HP-3318");
        p.setValorItens(new BigDecimal("200.00"));
        p.setDataPedido(LocalDateTime.of(2026, 11, 26, 20, 30));
        return p;
    }

    private static Compra compra(Long idEndereco) {
        Compra c = new Compra();
        c.setId(ID_COMPRA);
        c.setIdComprador(COMPRADOR);
        c.setIdEnderecoEntrega(idEndereco);
        c.setValorTotal(new BigDecimal("200.00"));
        c.setDataCompra(LocalDateTime.of(2026, 11, 26, 20, 30));
        return c;
    }

    private static UsuarioResponse comprador() {
        return new UsuarioResponse(COMPRADOR, "Carlos Pereira", "Oficina Rumo Certo", "carlos@example.com",
                TipoPessoa.PJ, "12345678000199", null, null, "19900000202", null, null, false,
                StatusUsuario.ATIVO, LocalDateTime.of(2026, 1, 1, 0, 0), List.of("COMPRADOR"));
    }

    private static EnderecoResponse endereco(Long id, String cidade, boolean principal) {
        return new EnderecoResponse(id, "Oficina", "13010110", "Rua A", "1", null, "Centro", cidade, "SP",
                null, null, principal);
    }

    private static PecaParaCompra peca(Long id, String titulo) {
        return new PecaParaCompra(id, LOJA, titulo, "OEM" + id, "SKU" + id, "Bosch", null,
                new BigDecimal("100.00"), 4, true, 1, true);
    }
}
