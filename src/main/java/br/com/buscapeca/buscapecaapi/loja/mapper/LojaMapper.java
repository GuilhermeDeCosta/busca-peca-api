package br.com.buscapeca.buscapecaapi.loja.mapper;

import br.com.buscapeca.buscapecaapi.loja.dto.LojaPublicaResponse;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaResponse;
import br.com.buscapeca.buscapecaapi.loja.entity.Loja;
import br.com.buscapeca.buscapecaapi.loja.enums.MetodoPagamento;
import br.com.buscapeca.buscapecaapi.usuario.dto.EnderecoResponse;
import br.com.buscapeca.buscapecaapi.usuario.dto.UsuarioResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/** Conversao Loja -> DTOs. O endereco e o dono vem dos servicos do modulo usuario. */
@Component
public class LojaMapper {

    public LojaResponse paraResponse(Loja l, EnderecoResponse endereco, UsuarioResponse dono) {
        return new LojaResponse(
                l.getId(),
                l.getNomeLoja(),
                l.getDescricao(),
                l.getTelefone(),
                dono.tipoPessoa(),
                dono.cnpjCpf(),
                endereco,
                endereco.cidade(),
                endereco.estado(),
                l.getLogoUrl(),
                l.getBannerUrl(),
                l.isAtiva(),
                l.getAvaliacaoMedia(),
                l.isVendedorVerificado(),
                l.isRetiradaBalcao(),
                l.getHorarioRetirada(),
                l.isEntregaCombinar(),
                l.isMotoboyProprio(),
                l.isEntregaRapida(),
                l.getFreteGratisRaioKm(),
                l.getPedidoMinimoFreteGratis(),
                l.isAceitaFaturamentoCnpj(),
                metodos(l),
                dono.nome(),
                dono.nomeExibicao(),
                l.getAtualizadoEm());
    }

    public LojaPublicaResponse paraPublica(Loja l, EnderecoResponse endereco) {
        return new LojaPublicaResponse(
                l.getId(),
                l.getNomeLoja(),
                l.getDescricao(),
                l.getLogoUrl(),
                l.getBannerUrl(),
                l.getAvaliacaoMedia(),
                l.isVendedorVerificado(),
                endereco.cidade(),
                endereco.estado(),
                l.isRetiradaBalcao(),
                l.getHorarioRetirada(),
                l.isEntregaCombinar(),
                l.isMotoboyProprio(),
                l.isEntregaRapida(),
                l.getFreteGratisRaioKm(),
                l.getPedidoMinimoFreteGratis(),
                l.isAceitaFaturamentoCnpj(),
                metodos(l));
    }

    /** Ordem fixa (a do enum), para a tela mostrar sempre igual. */
    private static List<MetodoPagamento> metodos(Loja l) {
        return l.getMetodosPagamento().stream().sorted().toList();
    }
}
