package br.com.buscapeca.buscapecaapi.pedido.repository;

import br.com.buscapeca.buscapecaapi.pedido.entity.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    Optional<Entrega> findByIdPedido(Long idPedido);
}
