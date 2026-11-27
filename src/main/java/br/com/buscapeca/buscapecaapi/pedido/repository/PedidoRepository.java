package br.com.buscapeca.buscapecaapi.pedido.repository;

import br.com.buscapeca.buscapecaapi.pedido.entity.Pedido;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Pedidos da compra, na ordem em que foram criados. */
    List<Pedido> findByIdCompraOrderByIdAsc(Long idCompra);

    /** Para gerar um codigo de retirada que ainda nao existe. */
    boolean existsByCodigoRetirada(String codigoRetirada);

    /**
     * O pedido com a linha bloqueada ate o fim da transacao: aceite e cancelamento ao mesmo
     * tempo nao passam os dois.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> travar(@Param("id") Long id);

    /** Pedidos recebidos pela loja (BP1-26). */
    Page<Pedido> findByIdLoja(Long idLoja, Pageable pagina);

    Page<Pedido> findByIdLojaAndStatus(Long idLoja, StatusPedido status, Pageable pagina);
}
