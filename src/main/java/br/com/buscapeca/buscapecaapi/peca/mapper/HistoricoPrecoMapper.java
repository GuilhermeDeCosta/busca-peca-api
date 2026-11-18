package br.com.buscapeca.buscapecaapi.peca.mapper;

import br.com.buscapeca.buscapecaapi.peca.dto.HistoricoPrecoResponse;
import br.com.buscapeca.buscapecaapi.peca.entity.HistoricoPreco;
import org.springframework.stereotype.Component;

/** Conversao HistoricoPreco -> DTO. */
@Component
public class HistoricoPrecoMapper {

    public HistoricoPrecoResponse paraResponse(HistoricoPreco h) {
        return new HistoricoPrecoResponse(h.getDataAlteracao(), h.getPrecoAnterior(), h.getPrecoNovo());
    }
}