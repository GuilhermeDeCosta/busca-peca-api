package br.com.buscapeca.buscapecaapi.peca.service;

import br.com.buscapeca.buscapecaapi.administracao.service.AuditoriaService;
import br.com.buscapeca.buscapecaapi.peca.dto.HistoricoPrecoResponse;
import br.com.buscapeca.buscapecaapi.peca.entity.HistoricoPreco;
import br.com.buscapeca.buscapecaapi.peca.mapper.HistoricoPrecoMapper;
import br.com.buscapeca.buscapecaapi.peca.repository.HistoricoPrecoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Historico de preco do anuncio (BP1-53).
 *
 * <p>Regra: so anuncio publicado (ativo ou pausado) entra no historico. A primeira linha e a
 * publicacao, com preco anterior nulo; depois, uma linha a cada preco diferente do ultimo
 * registrado. A comparacao e com o historico, e nao com o status anterior, para que voltar a
 * rascunho e publicar de novo nao crie uma segunda linha de publicacao.
 *
 * <p>Nao conhece a entidade Peca: quem chama (PecaService) informa se a peca esta publicada
 * e confere se o usuario e dono da loja antes de listar.
 */
@Service
public class HistoricoPrecoService {

    private final HistoricoPrecoRepository repository;
    private final AuditoriaService auditoriaService;
    private final HistoricoPrecoMapper mapper;

    public HistoricoPrecoService(HistoricoPrecoRepository repository, AuditoriaService auditoriaService,
                                 HistoricoPrecoMapper mapper) {
        this.repository = repository;
        this.auditoriaService = auditoriaService;
        this.mapper = mapper;
    }

    /**
     * Grava uma linha se o preco da peca publicada mudou. Roda dentro da transacao de quem
     * chamou: se o salvamento da peca for desfeito, a linha tambem e.
     *
     * @return verdadeiro se gravou uma linha
     */
    @Transactional
    public boolean registrarSeMudou(Long idUsuario, Long idPeca, boolean publicado, BigDecimal preco) {
        if (!publicado || preco == null) {
            return false;
        }
        BigDecimal anterior = repository.findFirstByIdPecaOrderByDataAlteracaoDescIdDesc(idPeca)
                .map(HistoricoPreco::getPrecoNovo)
                .orElse(null);
        // compareTo, e nao equals: para o BigDecimal, 749.0 e 749.00 sao diferentes no equals
        if (anterior != null && anterior.compareTo(preco) == 0) {
            return false;
        }
        repository.save(new HistoricoPreco(idPeca, anterior, preco));
        if (anterior != null) {
            auditoriaService.registrar(idUsuario, "ALTERAR_PRECO", "peca",
                    Map.of("idPeca", idPeca, "precoAnterior", anterior, "precoNovo", preco));
        }
        return true;
    }

    /** Historico da peca, da mudanca mais recente para a publicacao. Vazio se nunca foi publicada. */
    @Transactional(readOnly = true)
    public List<HistoricoPrecoResponse> listar(Long idPeca) {
        return repository.findByIdPecaOrderByDataAlteracaoDescIdDesc(idPeca).stream()
                .map(mapper::paraResponse)
                .toList();
    }
}