package br.com.buscapeca.buscapecaapi.peca.service;

import br.com.buscapeca.buscapecaapi.catalogo.service.CatalogoService;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.AcessoNegadoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.peca.dto.HistoricoPrecoResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaRequest;
import br.com.buscapeca.buscapecaapi.peca.entity.Peca;
import br.com.buscapeca.buscapecaapi.peca.enums.CondicaoPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.OrigemPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;
import br.com.buscapeca.buscapecaapi.peca.mapper.PecaMapper;
import br.com.buscapeca.buscapecaapi.peca.repository.CompatibilidadeRepository;
import br.com.buscapeca.buscapecaapi.peca.repository.PecaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Ligacao do PecaService com o historico de preco (BP1-53): o que o cadastro e a edicao
 * passam para o HistoricoPrecoService e quem pode ver o historico. Sem banco: tudo simulado.
 * As regras de quando gravar uma linha estao em HistoricoPrecoServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class PecaServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_LOJA = 7L;
    private static final Long ID_PECA = 10L;
    private static final Integer ID_CATEGORIA = 5;

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

    private PecaService service;

    @BeforeEach
    void preparar() {
        service = new PecaService(pecaRepository, compatibilidadeRepository, lojaService, catalogoService,
                mapper, historicoPrecoService);
    }

    // ----- Cadastro e edicao chamam o historico -----

    @Test
    @DisplayName("criar rascunho avisa o historico que a peca nao esta publicada")
    void criarRascunho() {
        temLoja();
        salvarDevolveComId();

        service.criar(ID_USUARIO, pedido(StatusPeca.RASCUNHO, null));

        verify(historicoPrecoService).registrarSeMudou(ID_USUARIO, ID_PECA, false, null);
    }

    @Test
    @DisplayName("criar ja publicado passa o id gerado e o preco para o historico")
    void criarPublicado() {
        temLoja();
        categoriaExiste();
        salvarDevolveComId();

        service.criar(ID_USUARIO, pedido(StatusPeca.ATIVO, "799.00"));

        assertThat(precoRegistrado(ID_PECA, true)).isEqualByComparingTo("799.00");
    }

    @Test
    @DisplayName("editar o preco de um anuncio pausado passa o preco novo para o historico")
    void atualizarPausado() {
        pecaExistente(ID_LOJA, StatusPeca.ATIVO, "799.00");
        temLoja();
        categoriaExiste();
        salvarDevolveComId();

        service.atualizar(ID_USUARIO, ID_PECA, pedido(StatusPeca.PAUSADO, "749.00"));

        assertThat(precoRegistrado(ID_PECA, true)).isEqualByComparingTo("749.00");
    }

    @Test
    @DisplayName("formulario invalido nao salva a peca nem mexe no historico")
    void dadosInvalidosNaoChamaHistorico() {
        temLoja();
        categoriaExiste();

        // Publicar sem preco: o rascunho aceita, o anuncio publicado nao
        assertThatThrownBy(() -> service.criar(ID_USUARIO, pedido(StatusPeca.ATIVO, null)))
                .isInstanceOf(DadosInvalidosException.class);

        verify(pecaRepository, never()).saveAndFlush(any());
        verifyNoInteractions(historicoPrecoService);
    }

    // ----- Quem pode ver o historico -----

    @Test
    @DisplayName("dono da peca recebe o historico")
    void historicoDoDono() {
        pecaExistente(ID_LOJA, StatusPeca.ATIVO, "749.00");
        temLoja();
        List<HistoricoPrecoResponse> linhas = List.of(
                new HistoricoPrecoResponse(LocalDateTime.of(2026, 11, 18, 10, 0),
                        new BigDecimal("799.00"), new BigDecimal("749.00")),
                new HistoricoPrecoResponse(LocalDateTime.of(2026, 11, 10, 9, 0),
                        null, new BigDecimal("799.00")));
        when(historicoPrecoService.listar(ID_PECA)).thenReturn(linhas);

        assertThat(service.historicoPreco(ID_USUARIO, ID_PECA)).isEqualTo(linhas);
    }

    @Test
    @DisplayName("peca de outra loja da 403 e nao le o historico")
    void historicoDeOutraLoja() {
        pecaExistente(99L, StatusPeca.ATIVO, "749.00");
        temLoja();

        assertThatThrownBy(() -> service.historicoPreco(ID_USUARIO, ID_PECA))
                .isInstanceOf(AcessoNegadoException.class);
        verify(historicoPrecoService, never()).listar(anyLong());
    }

    @Test
    @DisplayName("usuario sem loja da 403 e nao le o historico")
    void historicoSemLoja() {
        pecaExistente(ID_LOJA, StatusPeca.ATIVO, "749.00");
        when(lojaService.idDaLojaParaAnunciar(ID_USUARIO))
                .thenThrow(new AcessoNegadoException("Crie sua loja antes de anunciar peças."));

        assertThatThrownBy(() -> service.historicoPreco(ID_USUARIO, ID_PECA))
                .isInstanceOf(AcessoNegadoException.class);
        verify(historicoPrecoService, never()).listar(anyLong());
    }

    @Test
    @DisplayName("peca excluida ou inexistente da 404 e nao le o historico")
    void historicoDePecaQueNaoExiste() {
        when(pecaRepository.findByIdAndStatusNot(ID_PECA, StatusPeca.EXCLUIDO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.historicoPreco(ID_USUARIO, ID_PECA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(historicoPrecoService);
    }

    // ----- Apoio -----

    private void temLoja() {
        when(lojaService.idDaLojaParaAnunciar(ID_USUARIO)).thenReturn(ID_LOJA);
    }

    private void categoriaExiste() {
        when(catalogoService.categoriaExiste(ID_CATEGORIA)).thenReturn(true);
    }

    /** Simula o banco: devolve a mesma peca, com id na primeira gravacao. */
    private void salvarDevolveComId() {
        when(pecaRepository.saveAndFlush(any(Peca.class))).thenAnswer(chamada -> {
            Peca peca = chamada.getArgument(0);
            if (peca.getId() == null) {
                peca.setId(ID_PECA);
            }
            return peca;
        });
    }

    private void pecaExistente(Long idLoja, StatusPeca status, String preco) {
        Peca peca = new Peca();
        peca.setId(ID_PECA);
        peca.setIdLoja(idLoja);
        peca.setStatus(status);
        peca.setPreco(new BigDecimal(preco));
        when(pecaRepository.findByIdAndStatusNot(ID_PECA, StatusPeca.EXCLUIDO)).thenReturn(Optional.of(peca));
    }

    /**
     * Formulario do form-peca. Rascunho vai sem categoria e sem OEM; o preco vazio chega
     * como 0, como o front manda.
     */
    private static PecaRequest pedido(StatusPeca status, String preco) {
        boolean rascunho = status == StatusPeca.RASCUNHO;
        return new PecaRequest(
                "Alternador Bosch 90A",
                null,
                rascunho ? null : ID_CATEGORIA,
                "Bosch",
                rascunho ? null : "ALT-90GM",
                null,
                null,
                "12 V • 90 A",
                CondicaoPeca.NOVA,
                OrigemPeca.NACIONAL,
                preco == null ? BigDecimal.ZERO : new BigDecimal(preco),
                3,
                true,
                0,
                status);
    }

    /** Confere a chamada ao historico e devolve o preco que foi passado. */
    private BigDecimal precoRegistrado(Long idPeca, boolean publicado) {
        ArgumentCaptor<BigDecimal> preco = ArgumentCaptor.forClass(BigDecimal.class);
        verify(historicoPrecoService).registrarSeMudou(eq(ID_USUARIO), eq(idPeca), eq(publicado), preco.capture());
        return preco.getValue();
    }
}