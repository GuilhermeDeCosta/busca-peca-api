package br.com.buscapeca.buscapecaapi.catalogo.repository;

import br.com.buscapeca.buscapecaapi.catalogo.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    List<Categoria> findAllByOrderByNomeAsc();
}
