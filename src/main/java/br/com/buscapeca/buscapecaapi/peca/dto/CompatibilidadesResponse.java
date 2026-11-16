package br.com.buscapeca.buscapecaapi.peca.dto;

import java.util.List;

public record CompatibilidadesResponse(Long idPeca, List<AplicacaoResponse> aplicacoes) {
}
