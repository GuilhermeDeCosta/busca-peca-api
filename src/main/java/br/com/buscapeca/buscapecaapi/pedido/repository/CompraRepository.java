package br.com.buscapeca.buscapecaapi.pedido.repository;

import br.com.buscapeca.buscapecaapi.pedido.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    /** A compra so se for deste comprador: a de outra pessoa vira 404. */
    Optional<Compra> findByIdAndIdComprador(Long id, Long idComprador);

    /**
     * Compras do comprador feitas no periodo [inicio, fim), da mais recente para a mais
     * antiga (GET /compras). Sem filtro de data, quem chama passa os limites do DATETIME.
     */
    List<Compra> findByIdCompradorAndDataCompraGreaterThanEqualAndDataCompraLessThanOrderByDataCompraDescIdDesc(
            Long idComprador, LocalDateTime inicio, LocalDateTime fim);
}
