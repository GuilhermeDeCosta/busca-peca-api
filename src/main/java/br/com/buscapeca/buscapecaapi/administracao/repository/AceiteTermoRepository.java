package br.com.buscapeca.buscapecaapi.administracao.repository;

import br.com.buscapeca.buscapecaapi.administracao.entity.AceiteTermo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AceiteTermoRepository extends JpaRepository<AceiteTermo, Long> {

    boolean existsByIdUsuarioAndTermoId(Long idUsuario, Integer idTermo);
}
