package br.com.buscapeca.buscapecaapi.peca.repository;

import br.com.buscapeca.buscapecaapi.peca.entity.Peca;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Specification: a listagem "Meus anuncios" monta os filtros em FiltroMeusAnuncios. */
public interface PecaRepository extends JpaRepository<Peca, Long>, JpaSpecificationExecutor<Peca> {

    /** Peca que ainda existe para o sistema (exclusao logica fica de fora). */
    Optional<Peca> findByIdAndStatusNot(Long id, StatusPeca status);

    /**
     * Bloqueia as linhas das pecas ate o fim da transacao (SELECT ... FOR UPDATE), para
     * duas compras nao levarem a ultima unidade (Documentacao do Back-end, secao 3).
     * Sempre na ordem do id, para duas compras ao mesmo tempo nao travarem uma a outra.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Peca p WHERE p.id IN :ids ORDER BY p.id")
    List<Peca> travarParaCompra(@Param("ids") Collection<Long> ids);
}
