package br.com.buscapeca.buscapecaapi.peca.mapper;

import br.com.buscapeca.buscapecaapi.catalogo.dto.VeiculoResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.AplicacaoResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaResponse;
import br.com.buscapeca.buscapecaapi.peca.entity.Peca;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Conversao Peca -> DTOs. Os veiculos vem do CatalogoService. */
@Component
public class PecaMapper {

    public PecaResponse paraResponse(Peca p, List<AplicacaoResponse> aplicacoes) {
        // Sem alteracao ainda, o "salvo em" da tela usa a data de cadastro
        var atualizadoEm = p.getAtualizadoEm() != null ? p.getAtualizadoEm() : p.getDataCadastro();
        return new PecaResponse(
                p.getId(),
                p.getIdLoja(),
                p.getTitulo(),
                texto(p.getDescricao()),
                p.getIdCategoria(),
                texto(p.getMarcaPeca()),
                texto(p.getCodigoOem()),
                texto(p.getCodigoEan()),
                texto(p.getCodigoSku()),
                texto(p.getCaracteristicas()),
                p.getCondicao(),
                p.getOrigem(),
                p.getPreco(),
                p.getQuantidadeEstoque(),
                p.isProntaEntrega(),
                p.getPrazoExpedicaoDias(),
                p.getStatus(),
                p.getDataCadastro(),
                atualizadoEm,
                aplicacoes);
    }

    /**
     * Junta os anos de cada marca/modelo/versao em faixas seguidas.
     * Ex.: Gol 2010, 2011, 2012 e 2015 viram 2010-2012 e 2015-2015.
     */
    public List<AplicacaoResponse> agruparEmFaixas(List<VeiculoResponse> veiculos) {
        Map<String, List<VeiculoResponse>> grupos = new TreeMap<>();
        for (VeiculoResponse v : veiculos) {
            String chave = v.marca() + "\u0000" + v.modelo() + "\u0000" + texto(v.versao());
            grupos.computeIfAbsent(chave, c -> new ArrayList<>()).add(v);
        }
        List<AplicacaoResponse> faixas = new ArrayList<>();
        for (List<VeiculoResponse> grupo : grupos.values()) {
            grupo.sort(Comparator.comparing(VeiculoResponse::ano));
            VeiculoResponse primeiro = grupo.getFirst();
            int inicio = primeiro.ano();
            int fim = inicio;
            for (VeiculoResponse v : grupo.subList(1, grupo.size())) {
                if (v.ano() == fim + 1) {
                    fim = v.ano();
                } else {
                    faixas.add(faixa(primeiro, inicio, fim));
                    inicio = v.ano();
                    fim = inicio;
                }
            }
            faixas.add(faixa(primeiro, inicio, fim));
        }
        return faixas;
    }

    private static AplicacaoResponse faixa(VeiculoResponse v, int inicio, int fim) {
        return new AplicacaoResponse(v.marca(), v.modelo(), inicio, fim, texto(v.versao()));
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}
