package br.com.buscapeca.buscapecaapi.administracao.dto;

import br.com.buscapeca.buscapecaapi.administracao.entity.TermoUso;
import br.com.buscapeca.buscapecaapi.administracao.enums.TipoTermo;

import java.time.LocalDateTime;

public record TermoResponse(
        Integer idTermo,
        TipoTermo tipo,
        String versao,
        String conteudo,
        LocalDateTime dataPublicacao
) {

    public static TermoResponse de(TermoUso termo) {
        return new TermoResponse(termo.getId(), termo.getTipo(), termo.getVersao(),
                termo.getConteudo(), termo.getDataPublicacao());
    }
}
