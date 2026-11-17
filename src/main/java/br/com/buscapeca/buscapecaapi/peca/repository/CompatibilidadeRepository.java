package br.com.buscapeca.buscapecaapi.peca.repository;

import br.com.buscapeca.buscapecaapi.peca.entity.Compatibilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CompatibilidadeRepository extends JpaRepository<Compatibilidade, Long> {

    List<Compatibilidade> findByIdPeca(Long idPeca);

    /**
     * Apaga na hora (antes dos inserts), para trocar a lista sem bater no
     * uk_compatibilidade (id_peca, id_veiculo).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Compatibilidade c where c.idPeca = :idPeca")
    void apagarDaPeca(@Param("idPeca") Long idPeca);
}
