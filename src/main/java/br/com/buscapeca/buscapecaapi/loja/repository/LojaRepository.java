package br.com.buscapeca.buscapecaapi.loja.repository;

import br.com.buscapeca.buscapecaapi.loja.entity.Loja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LojaRepository extends JpaRepository<Loja, Long> {

    /** A loja do usuario (cada conta tem no maximo uma, uk_loja_usuario). */
    Optional<Loja> findByIdUsuario(Long idUsuario);

    /** Usado para impedir uma segunda loja na mesma conta. */
    boolean existsByIdUsuario(Long idUsuario);

    /** Pagina publica: so lojas ativas. */
    Optional<Loja> findByIdAndAtivaTrue(Long idLoja);
}
