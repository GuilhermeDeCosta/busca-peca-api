package br.com.buscapeca.buscapecaapi.peca.dto;

/** Faixa de anos montada a partir das linhas de compatibilidade. versao vem "" quando nao tem. */
public record AplicacaoResponse(String marca, String modelo, int anoInicial, int anoFinal, String versao) {
}
