package br.com.buscapeca.buscapecaapi.pedido.repository;

import br.com.buscapeca.buscapecaapi.pedido.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    /** A compra so se for deste comprador: a de outra pessoa vira 404. */
    Optional<Compra> findByIdAndIdComprador(Long id, Long idComprador);
}
