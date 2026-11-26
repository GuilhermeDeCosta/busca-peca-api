package br.com.buscapeca.buscapecaapi.peca.repository;

import br.com.buscapeca.buscapecaapi.peca.entity.Peca;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** Specification: a listagem "Meus anuncios" monta os filtros em FiltroMeusAnuncios. */
public interface PecaRepository extends JpaRepository<Peca, Long>, JpaSpecificationExecutor<Peca> {

    /** Peca que ainda existe para o sistema (exclusao logica fica de fora). */
    Optional<Peca> findByIdAndStatusNot(Long id, StatusPeca status);
}
