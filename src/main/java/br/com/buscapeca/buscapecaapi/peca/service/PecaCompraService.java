package br.com.buscapeca.buscapecaapi.peca.service;

import br.com.buscapeca.buscapecaapi.compartilhado.excecao.ConflitoException;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import br.com.buscapeca.buscapecaapi.peca.entity.Peca;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;
import br.com.buscapeca.buscapecaapi.peca.repository.PecaRepository;
import br.com.buscapeca.buscapecaapi.peca.repository.PecaVitrineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * O que o carrinho e o checkout precisam saber das pecas: preco que vale agora, estoque e
 * se o anuncio pode ser comprado. Tambem e aqui que o checkout trava as linhas e baixa o
 * estoque. Outros modulos chamam este servico e nunca o PecaRepository (ADR-003).
 */
@Service
public class PecaCompraService {

    private final PecaRepository pecaRepository;
    private final PecaVitrineRepository vitrineRepository;

    public PecaCompraService(PecaRepository pecaRepository, PecaVitrineRepository vitrineRepository) {
        this.pecaRepository = pecaRepository;
        this.vitrineRepository = vitrineRepository;
    }

    /** Uma peca. Vazio se o id nao existe; excluida volta com disponivel = false. */
    @Transactional(readOnly = true)
    public Optional<PecaParaCompra> buscar(Long idPeca) {
        return Optional.ofNullable(buscarVarias(Set.of(idPeca)).get(idPeca));
    }

    /**
     * Varias pecas de uma vez (idPeca -> dados), para montar o carrinho sem uma consulta
     * por item. Ids que nao existem ficam de fora do mapa.
     */
    @Transactional(readOnly = true)
    public Map<Long, PecaParaCompra> buscarVarias(Collection<Long> idsPeca) {
        if (idsPeca.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return montar(pecaRepository.findAllById(idsPeca), idsPeca);
    }

    /**
     * Mesmo resultado de buscarVarias, mas com as linhas das pecas bloqueadas ate o fim da
     * transacao do checkout (BP1-24). Quem chama precisa estar numa transacao.
     */
    @Transactional
    public Map<Long, PecaParaCompra> travarParaCompra(Collection<Long> idsPeca) {
        if (idsPeca.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return montar(pecaRepository.travarParaCompra(idsPeca), idsPeca);
    }

    /**
     * Baixa o estoque das pecas compradas (idPeca -> quantidade), na transacao do checkout.
     * As linhas ja estao bloqueadas pelo travarParaCompra; a conferencia aqui e so uma
     * garantia a mais para o estoque nunca ficar negativo.
     */
    @Transactional
    public void baixarEstoque(Map<Long, Integer> quantidades) {
        if (quantidades.isEmpty()) {
            return;
        }
        List<Peca> pecas = pecaRepository.travarParaCompra(quantidades.keySet());
        for (Peca p : pecas) {
            int comprada = quantidades.get(p.getId());
            if (comprada > p.getQuantidadeEstoque()) {
                throw new ConflitoException("ESTOQUE_INSUFICIENTE", String.format(
                        "O estoque de \"%s\" mudou. Ajuste a quantidade no carrinho.", p.getTitulo()));
            }
            p.setQuantidadeEstoque(p.getQuantidadeEstoque() - comprada);
        }
        pecaRepository.saveAll(pecas);
    }

    private Map<Long, PecaParaCompra> montar(List<Peca> pecas, Collection<Long> idsPeca) {
        Map<Long, PecaParaCompra> resultado = new LinkedHashMap<>();
        Map<Long, BigDecimal> promocionais = vitrineRepository.precosPromocionais(idsPeca);
        Map<Long, String> fotos = vitrineRepository.fotosPrincipais(idsPeca);
        for (Peca p : pecas) {
            resultado.put(p.getId(), new PecaParaCompra(
                    p.getId(),
                    p.getIdLoja(),
                    p.getTitulo(),
                    p.getCodigoOem(),
                    p.getCodigoSku(),
                    p.getMarcaPeca(),
                    fotos.get(p.getId()),
                    precoEfetivo(p.getPreco(), promocionais.get(p.getId())),
                    p.getQuantidadeEstoque(),
                    p.isProntaEntrega(),
                    p.getPrazoExpedicaoDias(),
                    p.getStatus() == StatusPeca.ATIVO && p.getPreco() != null));
        }
        return resultado;
    }

    /** Mesma regra da busca: o promocional vigente, quando houver, senao o normal. */
    private static BigDecimal precoEfetivo(BigDecimal normal, BigDecimal promocional) {
        BigDecimal preco = promocional != null ? promocional : normal;
        return preco == null ? null : preco.setScale(2, RoundingMode.HALF_UP);
    }
}
