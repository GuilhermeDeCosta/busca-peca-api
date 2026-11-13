package br.com.buscapeca.buscapecaapi.administracao.service;

import br.com.buscapeca.buscapecaapi.administracao.dto.TermoResponse;
import br.com.buscapeca.buscapecaapi.administracao.entity.AceiteTermo;
import br.com.buscapeca.buscapecaapi.administracao.entity.TermoUso;
import br.com.buscapeca.buscapecaapi.administracao.repository.AceiteTermoRepository;
import br.com.buscapeca.buscapecaapi.administracao.repository.TermoUsoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Termos de uso e politica de privacidade (BP1-54, ADR-013).
 * Outros modulos usam este servico, nunca os repositorios.
 */
@Service
public class TermoUsoService {

    private final TermoUsoRepository termoRepository;
    private final AceiteTermoRepository aceiteRepository;

    public TermoUsoService(TermoUsoRepository termoRepository, AceiteTermoRepository aceiteRepository) {
        this.termoRepository = termoRepository;
        this.aceiteRepository = aceiteRepository;
    }

    @Transactional(readOnly = true)
    public List<TermoResponse> listarVigentes() {
        return termoRepository.findByVigenteTrueOrderByTipo().stream().map(TermoResponse::de).toList();
    }

    /** Registra o aceite de todas as versoes vigentes (cadastro e novo aceite). */
    @Transactional
    public void registrarAceiteDosVigentes(Long idUsuario) {
        LocalDateTime agora = LocalDateTime.now();
        for (TermoUso termo : termoRepository.findByVigenteTrueOrderByTipo()) {
            if (!aceiteRepository.existsByIdUsuarioAndTermoId(idUsuario, termo.getId())) {
                AceiteTermo aceite = new AceiteTermo();
                aceite.setIdUsuario(idUsuario);
                aceite.setTermo(termo);
                aceite.setDataAceite(agora);
                aceiteRepository.save(aceite);
            }
        }
    }

    /** Verdadeiro quando o usuario ainda nao aceitou alguma versao vigente. */
    @Transactional(readOnly = true)
    public boolean temAceitePendente(Long idUsuario) {
        return termoRepository.findByVigenteTrueOrderByTipo().stream()
                .anyMatch(t -> !aceiteRepository.existsByIdUsuarioAndTermoId(idUsuario, t.getId()));
    }
}
