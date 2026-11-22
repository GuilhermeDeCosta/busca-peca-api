package br.com.buscapeca.buscapecaapi.peca.service;

import br.com.buscapeca.buscapecaapi.administracao.service.AuditoriaService;
import br.com.buscapeca.buscapecaapi.catalogo.service.CatalogoService;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.AcessoNegadoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.ConflitoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.compartilhado.paginacao.PaginaResposta;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.peca.dto.MeuAnuncioResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.MeusAnunciosFiltros;
import br.com.buscapeca.buscapecaapi.peca.entity.Peca;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;
import br.com.buscapeca.buscapecaapi.peca.mapper.PecaMapper;
import br.com.buscapeca.buscapecaapi.peca.repository.CompatibilidadeRepository;
import br.com.buscapeca.buscapecaapi.peca.repository.PecaRepository;
import br.com.buscapeca.buscapecaapi.pedido.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Painel Minha Loja: listar, pausar, reativar e excluir os proprios anuncios
 * (BP1-225, 226, 227). Sem banco: tudo simulado. Os filtros viram SQL no
 * FiltroMeusAnuncios, que so da para conferir com o MySQL rodando.
 */
@ExtendWith(MockitoExtension.class)
class PecaAnunciosServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_LOJA = 7L;
    private static final Long ID_PECA = 10L;

    @Mock
    private PecaRepository pecaRepository;

    @Mock
    private CompatibilidadeRepository compatibilidadeRepository;

    @Mock
    private LojaService lojaService;

    @Mock
    private CatalogoService catalogoService;

    @Mock
    private PecaMapper mapper;

    @Mock
    private HistoricoPrecoService historicoPrecoService;

    @Mock
    private PedidoService pedidoService;

    @Mock
    private AuditoriaService auditoriaService;

    private PecaService service;

    @BeforeEach
    void preparar() {
        service = new PecaService(pecaRepository, compatibilidadeRepository, lojaService, catalogoService,
                mapper, historicoPrecoService, pedidoService, auditoriaService);
    }

    // ----- Listar meus anuncios (BP1-225) -----

    @Test
    @DisplayName("listar converte a pagina 1 do front na pagina 0 do banco e devolve a pagina 1")
    void listarPrimeiraPagina() {
        temLoja();
        Peca peca = peca(ID_LOJA, StatusPeca.ATIVO, 3);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(pecaRepository.findAll(any(Specification.class), pageable.capture()))
                .thenAnswer(chamada -> new PageImpl<>(List.of(peca), chamada.getArgument(1), 1));

        PaginaResposta<MeuAnuncioResponse> resposta =
                service.listarMinhas(ID_USUARIO, filtros(null, null, null, 1, 5));

        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(resposta.pagina()).isEqualTo(1);
        assertThat(resposta.totalRegistros()).isEqualTo(1);
        assertThat(resposta.totalPaginas()).isEqualTo(1);
        verify(mapper).paraMeuAnuncio(peca);
    }

    @Test
    @DisplayName("listar sem pagina usa a pagina 1 e o tamanho padrao de 20")
    void listarSemPagina() {
        temLoja();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(pecaRepository.findAll(any(Specification.class), pageable.capture()))
                .thenAnswer(chamada -> Page.empty(chamada.getArgument(1)));

        PaginaResposta<MeuAnuncioResponse> resposta =
                service.listarMinhas(ID_USUARIO, filtros(null, "", "", null, null));

        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(resposta.pagina()).isEqualTo(1);
        assertThat(resposta.itens()).isEmpty();
    }

    @Test
    @DisplayName("listar aceita status e estoque em minusculas, como o painel pode mandar")
    void listarFiltrosEmMinusculas() {
        temLoja();
        when(pecaRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(chamada -> Page.empty(chamada.getArgument(1)));

        service.listarMinhas(ID_USUARIO, filtros("alternador", "ativo", "baixo", 1, 5));

        verify(pecaRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("status EXCLUIDO ou estoque desconhecido dao 400 nos dois campos, sem consultar o banco")
    void listarFiltrosInvalidos() {
        temLoja();

        assertThatThrownBy(() -> service.listarMinhas(ID_USUARIO, filtros(null, "EXCLUIDO", "POUCO", 1, 5)))
                .isInstanceOfSatisfying(DadosInvalidosException.class,
                        e -> assertThat(e.getCampos()).containsOnlyKeys("status", "estoque"));
        verify(pecaRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("usuario sem loja da 403 na listagem")
    void listarSemLoja() {
        when(lojaService.idDaLojaParaAnunciar(ID_USUARIO))
                .thenThrow(new AcessoNegadoException("Crie sua loja antes de anunciar peças."));

        assertThatThrownBy(() -> service.listarMinhas(ID_USUARIO, filtros(null, null, null, 1, 5)))
                .isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(pecaRepository);
    }

    // ----- Pausar e reativar (BP1-226) -----

    @Test
    @DisplayName("pausar um anuncio ativo grava PAUSADO")
    void pausarAtivo() {
        Peca peca = pecaExistente(ID_LOJA, StatusPeca.ATIVO, 3);
        temLoja();
        salvarDevolveAMesma();

        service.pausar(ID_USUARIO, ID_PECA);

        assertThat(peca.getStatus()).isEqualTo(StatusPeca.PAUSADO);
        verify(mapper).paraStatus(peca);
    }

    @Test
    @DisplayName("pausar o que nao esta ativo da 409 STATUS_INVALIDO e nao salva")
    void pausarNaoAtivo() {
        pecaExistente(ID_LOJA, StatusPeca.PAUSADO, 3);
        temLoja();

        assertThatThrownBy(() -> service.pausar(ID_USUARIO, ID_PECA))
                .isInstanceOf(ConflitoException.class)
                .hasFieldOrPropertyWithValue("codigo", "STATUS_INVALIDO");
        verify(pecaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("reativar um anuncio pausado com estoque grava ATIVO")
    void reativarPausado() {
        Peca peca = pecaExistente(ID_LOJA, StatusPeca.PAUSADO, 2);
        temLoja();
        salvarDevolveAMesma();

        service.reativar(ID_USUARIO, ID_PECA);

        assertThat(peca.getStatus()).isEqualTo(StatusPeca.ATIVO);
    }

    @Test
    @DisplayName("reativar sem estoque da 409 ESTOQUE_ZERADO e continua pausado")
    void reativarSemEstoque() {
        Peca peca = pecaExistente(ID_LOJA, StatusPeca.PAUSADO, 0);
        temLoja();

        assertThatThrownBy(() -> service.reativar(ID_USUARIO, ID_PECA))
                .isInstanceOf(ConflitoException.class)
                .hasFieldOrPropertyWithValue("codigo", "ESTOQUE_ZERADO");
        assertThat(peca.getStatus()).isEqualTo(StatusPeca.PAUSADO);
        verify(pecaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("reativar um rascunho da 409 STATUS_INVALIDO: rascunho se publica pelo formulario")
    void reativarRascunho() {
        pecaExistente(ID_LOJA, StatusPeca.RASCUNHO, 5);
        temLoja();

        assertThatThrownBy(() -> service.reativar(ID_USUARIO, ID_PECA))
                .isInstanceOf(ConflitoException.class)
                .hasFieldOrPropertyWithValue("codigo", "STATUS_INVALIDO");
    }

    @Test
    @DisplayName("pausar anuncio de outra loja da 403")
    void pausarDeOutraLoja() {
        pecaExistente(99L, StatusPeca.ATIVO, 3);
        temLoja();

        assertThatThrownBy(() -> service.pausar(ID_USUARIO, ID_PECA))
                .isInstanceOf(AcessoNegadoException.class);
        verify(pecaRepository, never()).saveAndFlush(any());
    }

    // ----- Excluir (BP1-227) -----

    @Test
    @DisplayName("excluir sem pedido em andamento grava EXCLUIDO e registra na auditoria")
    void excluir() {
        Peca peca = pecaExistente(ID_LOJA, StatusPeca.PAUSADO, 3);
        temLoja();
        when(pedidoService.pedidosEmAndamentoComPeca(ID_PECA)).thenReturn(List.of());

        service.excluir(ID_USUARIO, ID_PECA);

        assertThat(peca.getStatus()).isEqualTo(StatusPeca.EXCLUIDO);
        verify(pecaRepository).saveAndFlush(peca);
        verify(auditoriaService).registrar(ID_USUARIO, "EXCLUIR_PECA", "peca",
                Map.of("idPeca", ID_PECA, "statusAnterior", "PAUSADO"));
    }

    @Test
    @DisplayName("excluir com pedido em andamento da 409 PEDIDOS_EM_ANDAMENTO com os codigos e nao exclui")
    void excluirComPedidoEmAndamento() {
        Peca peca = pecaExistente(ID_LOJA, StatusPeca.ATIVO, 3);
        temLoja();
        when(pedidoService.pedidosEmAndamentoComPeca(ID_PECA)).thenReturn(List.of(2081L));

        assertThatThrownBy(() -> service.excluir(ID_USUARIO, ID_PECA))
                .isInstanceOf(ConflitoException.class)
                .hasFieldOrPropertyWithValue("codigo", "PEDIDOS_EM_ANDAMENTO")
                .hasMessageContaining("um pedido em andamento (BP-2081)");
        assertThat(peca.getStatus()).isEqualTo(StatusPeca.ATIVO);
        verify(pecaRepository, never()).saveAndFlush(any());
        verify(auditoriaService, never()).registrar(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("excluir o que ja foi excluido da 404")
    void excluirDuasVezes() {
        when(pecaRepository.findByIdAndStatusNot(ID_PECA, StatusPeca.EXCLUIDO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(ID_USUARIO, ID_PECA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(pedidoService, auditoriaService);
    }

    @Test
    @DisplayName("mensagem com mais de um pedido lista todos os codigos")
    void mensagemVariosPedidos() {
        assertThat(PecaService.mensagemPedidosEmAndamento(List.of(2079L, 2081L)))
                .startsWith("Esta peça tem 2 pedidos em andamento (BP-2079, BP-2081).");
    }

    // ----- Apoio -----

    private void temLoja() {
        when(lojaService.idDaLojaParaAnunciar(ID_USUARIO)).thenReturn(ID_LOJA);
    }

    private void salvarDevolveAMesma() {
        when(pecaRepository.saveAndFlush(any(Peca.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private Peca pecaExistente(Long idLoja, StatusPeca status, int estoque) {
        Peca peca = peca(idLoja, status, estoque);
        when(pecaRepository.findByIdAndStatusNot(ID_PECA, StatusPeca.EXCLUIDO)).thenReturn(Optional.of(peca));
        return peca;
    }

    private static Peca peca(Long idLoja, StatusPeca status, int estoque) {
        Peca peca = new Peca();
        peca.setId(ID_PECA);
        peca.setIdLoja(idLoja);
        peca.setTitulo("Alternador Bosch 90A");
        peca.setStatus(status);
        peca.setPreco(new BigDecimal("749.00"));
        peca.setQuantidadeEstoque(estoque);
        return peca;
    }

    private static MeusAnunciosFiltros filtros(String q, String status, String estoque, Integer pagina, Integer tamanho) {
        return new MeusAnunciosFiltros("minha", q, status, estoque, pagina, tamanho);
    }
}
