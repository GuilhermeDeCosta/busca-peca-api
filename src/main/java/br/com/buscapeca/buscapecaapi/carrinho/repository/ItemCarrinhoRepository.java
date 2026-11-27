package br.com.buscapeca.buscapecaapi.carrinho.repository;

import br.com.buscapeca.buscapecaapi.carrinho.entity.ItemCarrinho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ItemCarrinhoRepository extends JpaRepository<ItemCarrinho, Long> {

    /** Itens na ordem em que entraram no carrinho. */
    List<ItemCarrinho> findByIdCarrinhoOrderByIdAsc(Long idCarrinho);

    /** A linha da peca no carrinho, para somar a quantidade. */
    Optional<ItemCarrinho> findByIdCarrinhoAndIdPeca(Long idCarrinho, Long idPeca);

    /** O item so se for deste carrinho: item de outro usuario vira 404. */
    Optional<ItemCarrinho> findByIdAndIdCarrinho(Long id, Long idCarrinho);

    @Modifying
    @Query("DELETE FROM ItemCarrinho i WHERE i.idCarrinho = :idCarrinho")
    void apagarTodos(@Param("idCarrinho") Long idCarrinho);
}
