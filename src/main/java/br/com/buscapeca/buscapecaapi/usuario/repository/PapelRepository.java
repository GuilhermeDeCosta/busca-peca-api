package br.com.buscapeca.buscapecaapi.usuario.repository;

import br.com.buscapeca.buscapecaapi.usuario.entity.Papel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PapelRepository extends JpaRepository<Papel, Integer> {

    Optional<Papel> findByNome(String nome);
}
