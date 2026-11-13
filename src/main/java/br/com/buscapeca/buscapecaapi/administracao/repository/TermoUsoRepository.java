package br.com.buscapeca.buscapecaapi.administracao.repository;

import br.com.buscapeca.buscapecaapi.administracao.entity.TermoUso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermoUsoRepository extends JpaRepository<TermoUso, Integer> {

    List<TermoUso> findByVigenteTrueOrderByTipo();
}
