package br.com.buscapeca.buscapecaapi.peca.repository;

import br.com.buscapeca.buscapecaapi.peca.entity.HistoricoPreco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Consultas do historico de preco. As duas usam o indice ix_historico_preco_peca_data (V13);
 * o id desempata mudancas gravadas no mesmo segundo.
 */
public interface HistoricoPrecoRepository extends JpaRepository<HistoricoPreco, Long> {

    /** Ultima mudanca da peca: o preco que esta valendo no historico. Vazio antes da publicacao. */
    Optional<HistoricoPreco> findFirstByIdPecaOrderByDataAlteracaoDescIdDesc(Long idPeca);

    /** Historico completo da peca, da mudanca mais recente para a mais antiga (a publicacao). */
    List<HistoricoPreco> findByIdPecaOrderByDataAlteracaoDescIdDesc(Long idPeca);
}