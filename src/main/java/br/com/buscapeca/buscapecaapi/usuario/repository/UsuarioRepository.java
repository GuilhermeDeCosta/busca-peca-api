package br.com.buscapeca.buscapecaapi.usuario.repository;

import br.com.buscapeca.buscapecaapi.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"papeis", "papeis.permissoes"})
    Optional<Usuario> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"papeis", "papeis.permissoes"})
    Optional<Usuario> findComPapeisById(Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCnpjCpf(String cnpjCpf);
}
