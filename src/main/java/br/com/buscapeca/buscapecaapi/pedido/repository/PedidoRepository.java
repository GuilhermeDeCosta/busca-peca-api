package br.com.buscapeca.buscapecaapi.pedido.repository;

import br.com.buscapeca.buscapecaapi.pedido.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Pedidos da compra, na ordem em que foram criados. */
    List<Pedido> findByIdCompraOrderByIdAsc(Long idCompra);

    /** Para gerar um codigo de retirada que ainda nao existe. */
    boolean existsByCodigoRetirada(String codigoRetirada);
}
