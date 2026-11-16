package br.com.buscapeca.buscapecaapi.usuario.repository;

import br.com.buscapeca.buscapecaapi.usuario.entity.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {

    /** Enderecos do usuario, com o principal primeiro. */
    List<Endereco> findByIdUsuarioOrderByPrincipalDescIdAsc(Long idUsuario);

    /** Quantos enderecos o usuario tem (o primeiro vira principal). */
    long countByIdUsuario(Long idUsuario);

    /** Endereco mais antigo, usado para virar principal quando o principal e removido. */
    Optional<Endereco> findFirstByIdUsuarioOrderByIdAsc(Long idUsuario);

    /** Tira a marca de principal de todos os enderecos do usuario. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Endereco e set e.principal = false where e.idUsuario = :idUsuario and e.principal = true")
    int desmarcarPrincipal(@Param("idUsuario") Long idUsuario);
}