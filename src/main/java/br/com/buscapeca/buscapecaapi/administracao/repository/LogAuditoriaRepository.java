package br.com.buscapeca.buscapecaapi.administracao.repository;

import br.com.buscapeca.buscapecaapi.administracao.entity.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {
}
