package br.com.buscapeca.buscapecaapi.peca.service;

import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import br.com.buscapeca.buscapecaapi.peca.entity.Peca;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;
import br.com.buscapeca.buscapecaapi.peca.repository.PecaRepository;
import br.com.buscapeca.buscapecaapi.peca.repository.PecaVitrineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Dados das pecas para o carrinho: preco promocional, foto e disponibilidade. Sem banco. */
@ExtendWith(MockitoExtension.class)
class PecaCompraServiceTest {

    @Mock
    private PecaRepository pecaRepository;

    @Mock
    private PecaVitrineRepository vitrineRepository;

    private PecaCompraService service;

    @BeforeEach
    void preparar() {
        service = new PecaCompraService(pecaRepository, vitrineRepository);
    }

    @Test
    @DisplayName("Usa o preco promocional vigente e a foto principal; ids que nao existem ficam de fora")
    void buscarVarias() {
        when(pecaRepository.findAllById(anyCollection()))
                .thenReturn(List.of(peca(10L, "100.00", StatusPeca.ATIVO), peca(11L, "50", StatusPeca.ATIVO)));
        when(vitrineRepository.precosPromocionais(anyCollection())).thenReturn(Map.of(10L, new BigDecimal("89.90")));
        when(vitrineRepository.fotosPrincipais(anyCollection())).thenReturn(Map.of(11L, "/uploads/11.jpg"));

        Map<Long, PecaParaCompra> pecas = service.buscarVarias(Set.of(10L, 11L, 99L));

        assertThat(pecas).containsOnlyKeys(10L, 11L);
        assertThat(pecas.get(10L).preco()).isEqualByComparingTo("89.90");
        assertThat(pecas.get(10L).fotoUrl()).isNull();
        assertThat(pecas.get(11L).preco()).isEqualTo(new BigDecimal("50.00"));
        assertThat(pecas.get(11L).fotoUrl()).isEqualTo("/uploads/11.jpg");
        assertThat(pecas.get(11L).idLoja()).isEqualTo(7L);
        assertThat(pecas.values()).allMatch(PecaParaCompra::disponivel);
    }

    @Test
    @DisplayName("Pausado, rascunho e excluido voltam com disponivel = false")
    void indisponiveis() {
        when(pecaRepository.findAllById(anyCollection())).thenReturn(List.of(
                peca(10L, "10", StatusPeca.PAUSADO),
                peca(11L, "10", StatusPeca.RASCUNHO),
                peca(12L, "10", StatusPeca.EXCLUIDO)));
        when(vitrineRepository.precosPromocionais(anyCollection())).thenReturn(Map.of());
        when(vitrineRepository.fotosPrincipais(anyCollection())).thenReturn(Map.of());

        Map<Long, PecaParaCompra> pecas = service.buscarVarias(Set.of(10L, 11L, 12L));

        assertThat(pecas.values()).noneMatch(PecaParaCompra::disponivel);
    }

    @Test
    @DisplayName("Lista vazia nao consulta o banco")
    void listaVazia() {
        assertThat(service.buscarVarias(Set.of())).isEmpty();
        verifyNoInteractions(pecaRepository, vitrineRepository);
    }

    private static Peca peca(Long id, String preco, StatusPeca status) {
        Peca p = new Peca();
        p.setId(id);
        p.setIdLoja(7L);
        p.setTitulo("Peça " + id);
        p.setPreco(new BigDecimal(preco));
        p.setQuantidadeEstoque(3);
        p.setStatus(status);
        return p;
    }
}
