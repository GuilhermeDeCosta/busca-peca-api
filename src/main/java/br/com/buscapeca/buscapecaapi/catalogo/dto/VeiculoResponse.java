package br.com.buscapeca.buscapecaapi.catalogo.dto;

import br.com.buscapeca.buscapecaapi.catalogo.entity.Veiculo;

public record VeiculoResponse(Long idVeiculo, String marca, String modelo, Short ano, String versao) {

    public static VeiculoResponse de(Veiculo v) {
        return new VeiculoResponse(v.getId(), v.getMarca(), v.getModelo(), v.getAno(), v.getVersao());
    }
}
