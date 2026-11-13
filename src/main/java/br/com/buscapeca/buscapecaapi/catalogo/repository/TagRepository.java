package br.com.buscapeca.buscapecaapi.catalogo.repository;

import br.com.buscapeca.buscapecaapi.catalogo.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Integer> {

    List<Tag> findAllByOrderByNomeAsc();
}
