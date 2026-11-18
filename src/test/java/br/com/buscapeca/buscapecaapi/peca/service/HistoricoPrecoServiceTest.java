package br.com.buscapeca.buscapecaapi.peca.service;

import br.com.buscapeca.buscapecaapi.administracao.service.AuditoriaService;
import br.com.buscapeca.buscapecaapi.peca.dto.HistoricoPrecoResponse;
import br.com.buscapeca.buscapecaapi.peca.entity.HistoricoPreco;
import br.com.buscapeca.buscapecaapi.peca.mapper.HistoricoPrecoMapper;
import br.com.buscapeca.buscapecaapi.peca.repository.HistoricoPrecoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Regras do historico de preco (BP1-53), sem banco: repositorio e auditoria sao simulados. */
@ExtendWith(MockitoExtension.class)
class HistoricoPrecoServiceTest {

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_PECA = 10L;

    @Mock
    private HistoricoPrecoRepository repository;

    @Mock
    private AuditoriaService auditoriaService;

    private HistoricoPrecoService service;

    @BeforeEach
    void preparar() {
        service = new HistoricoPrecoService(repository, auditoriaService, new HistoricoPrecoMapper());
    }

    @Test
    @DisplayName("rascunho nao entra no historico")
    void naoPublicadoNaoGrava() {
        boolean gravou = service.registrarSeMudou(ID_USUARIO, ID_PECA, false, new BigDecimal("749.00"));

        assertThat(gravou).isFalse();
        verifyNoInteractions(repository, auditoriaService);
    }

    @Test
    @DisplayName("peca publicada sem preco nao entra no historico")
    void semPrecoNaoGrava() {
        boolean gravou = service.registrarSeMudou(ID_USUARIO, ID_PECA, true, null);

        assertThat(gravou).isFalse();
        verifyNoInteractions(repository, auditoriaService);
    }

    @Test
    @DisplayName("primeira publicacao grava a linha com preco anterior nulo e sem auditoria")
    void primeiraPublicacaoGravaAnteriorNulo() {
        when(repository.findFirstByIdPecaOrderByDataAlteracaoDescIdDesc(ID_PECA)).thenReturn(Optional.empty());

        boolean gravou = service.registrarSeMudou(ID_USUARIO, ID_PECA, true, new BigDecimal("829.00"));

        assertThat(gravou).isTrue();
        HistoricoPreco salvo = linhaSalva();
        assertThat(salvo.getIdPeca()).isEqualTo(ID_PECA);
        assertThat(salvo.getPrecoAnterior()).isNull();
        assertThat(salvo.getPrecoNovo()).isEqualByComparingTo("829.00");
        verify(auditoriaService, never()).registrar(any(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("preco igual ao ultimo nao grava, mesmo com casas decimais diferentes")
    void mesmoPrecoNaoGrava() {
        when(repository.findFirstByIdPecaOrderByDataAlteracaoDescIdDesc(ID_PECA))
                .thenReturn(Optional.of(linha(null, "749.00", 0)));

        boolean gravou = service.registrarSeMudou(ID_USUARIO, ID_PECA, true, new BigDecimal("749.0"));

        assertThat(gravou).isFalse();
        verify(repository, never()).save(any());
        verifyNoInteractions(auditoriaService);
    }

    @Test
    @DisplayName("preco diferente grava anterior e novo e registra ALTERAR_PRECO")
    void precoDiferenteGravaEAudita() {
        when(repository.findFirstByIdPecaOrderByDataAlteracaoDescIdDesc(ID_PECA))
                .thenReturn(Optional.of(linha(null, "799.00", 0)));
        BigDecimal novo = new BigDecimal("749.00");

        boolean gravou = service.registrarSeMudou(ID_USUARIO, ID_PECA, true, novo);

        assertThat(gravou).isTrue();
        HistoricoPreco salvo = linhaSalva();
        assertThat(salvo.getPrecoAnterior()).isEqualByComparingTo("799.00");
        assertThat(salvo.getPrecoNovo()).isEqualByComparingTo("749.00");
        verify(auditoriaService).registrar(ID_USUARIO, "ALTERAR_PRECO", "peca",
                Map.of("idPeca", ID_PECA, "precoAnterior", new BigDecimal("799.00"), "precoNovo", novo));
    }

    @Test
    @DisplayName("listar devolve as linhas convertidas na ordem do repositorio")
    void listarConverteNaOrdem() {
        HistoricoPreco mudanca = linha("799.00", "749.00", 1);
        HistoricoPreco publicacao = linha(null, "799.00", 5);
        when(repository.findByIdPecaOrderByDataAlteracaoDescIdDesc(ID_PECA)).thenReturn(List.of(mudanca, publicacao));

        List<HistoricoPrecoResponse> linhas = service.listar(ID_PECA);

        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0).data()).isEqualTo(mudanca.getDataAlteracao());
        assertThat(linhas.get(0).precoAnterior()).isEqualByComparingTo("799.00");
        assertThat(linhas.get(0).precoNovo()).isEqualByComparingTo("749.00");
        assertThat(linhas.get(1).precoAnterior()).isNull();
        assertThat(linhas.get(1).precoNovo()).isEqualByComparingTo("799.00");
    }

    @Test
    @DisplayName("listar devolve lista vazia para peca que nunca foi publicada")
    void listarVazio() {
        when(repository.findByIdPecaOrderByDataAlteracaoDescIdDesc(ID_PECA)).thenReturn(List.of());

        assertThat(service.listar(ID_PECA)).isEmpty();
    }

    // ----- Apoio -----

    /** Linha de historico como viria do banco, com a data de alguns dias atras. */
    private static HistoricoPreco linha(String anterior, String novo, int diasAtras) {
        HistoricoPreco h = new HistoricoPreco(ID_PECA,
                anterior == null ? null : new BigDecimal(anterior), new BigDecimal(novo));
        ReflectionTestUtils.setField(h, "dataAlteracao", LocalDateTime.of(2026, 11, 18, 10, 0).minusDays(diasAtras));
        return h;
    }

    private HistoricoPreco linhaSalva() {
        ArgumentCaptor<HistoricoPreco> captor = ArgumentCaptor.forClass(HistoricoPreco.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}