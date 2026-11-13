package br.com.buscapeca.buscapecaapi.usuario.repository;

import br.com.buscapeca.buscapecaapi.usuario.entity.TokenVerificacao;
import br.com.buscapeca.buscapecaapi.usuario.enums.TipoToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenVerificacaoRepository extends JpaRepository<TokenVerificacao, Long> {

    Optional<TokenVerificacao> findByTokenHashAndTipo(String tokenHash, TipoToken tipo);

    /** Invalida os links anteriores quando um novo e pedido. */
    @Modifying
    @Query("update TokenVerificacao t set t.usado = true "
            + "where t.usuario.id = :idUsuario and t.tipo = :tipo and t.usado = false")
    int invalidarAnteriores(@Param("idUsuario") Long idUsuario, @Param("tipo") TipoToken tipo);
}
