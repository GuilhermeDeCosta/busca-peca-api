package br.com.buscapeca.buscapecaapi.peca.repository;

import br.com.buscapeca.buscapecaapi.peca.entity.Peca;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PecaRepository extends JpaRepository<Peca, Long> {

    /** Peca que ainda existe para o sistema (exclusao logica fica de fora). */
    Optional<Peca> findByIdAndStatusNot(Long id, StatusPeca status);
}
