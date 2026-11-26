package br.com.buscapeca.buscapecaapi.integracao.frete;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Implementacao do MVP: nenhuma transportadora integrada, entao toda loja fica com o
 * frete a combinar (ADR-008, ADR-012). E o mesmo caminho de quando o servico de frete
 * cai, e o que o carrinho do front ja mostra. Quando o time escolher um servico, a nova
 * implementacao entra no perfil prod e esta fica para o dev.
 */
@Service
public class FreteServiceACombinar implements FreteService {

    @Override
    public Optional<List<OpcaoFrete>> cotar(Long idLoja, String cepDestino, BigDecimal valorItens) {
        return Optional.empty();
    }
}
