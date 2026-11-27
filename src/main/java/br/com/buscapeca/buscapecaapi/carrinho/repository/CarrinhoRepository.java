package br.com.buscapeca.buscapecaapi.carrinho.repository;

import br.com.buscapeca.buscapecaapi.carrinho.entity.Carrinho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarrinhoRepository extends JpaRepository<Carrinho, Long> {

    /** O carrinho do usuario (no maximo um, uk_carrinho_usuario). */
    Optional<Carrinho> findByIdUsuario(Long idUsuario);
}
