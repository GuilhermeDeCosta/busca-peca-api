package br.com.buscapeca.buscapecaapi.peca.service;

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
 * O que o carrinho (e depois o checkout) precisa saber das pecas: preco que vale agora,
 * estoque e se o anuncio pode ser comprado. Outros modulos chamam este servico e nunca
 * o PecaRepository (ADR-003).
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
        Map<Long, PecaParaCompra> resultado = new LinkedHashMap<>();
        if (idsPeca.isEmpty()) {
            return resultado;
        }
        List<Peca> pecas = pecaRepository.findAllById(idsPeca);
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
