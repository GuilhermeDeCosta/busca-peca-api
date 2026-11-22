package br.com.buscapeca.buscapecaapi.pedido.service;

import br.com.buscapeca.buscapecaapi.pedido.repository.PedidoConsultaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servico do modulo de pedidos (Documentacao do Back-end, secao 5.8). Por enquanto so
 * responde o que outros modulos precisam saber sobre pedidos (ADR-003); checkout,
 * aceite, recusa e entrega entram aqui com o modulo de pedidos.
 */
@Service
public class PedidoService {

    private final PedidoConsultaRepository consultaRepository;

    public PedidoService(PedidoConsultaRepository consultaRepository) {
        this.consultaRepository = consultaRepository;
    }

    /**
     * Pedidos aguardando aceite, aceitos, prontos para retirada ou em entrega que tem a
     * peca. Usado para impedir a exclusao do anuncio (BP1-227).
     */
    @Transactional(readOnly = true)
    public List<Long> pedidosEmAndamentoComPeca(Long idPeca) {
        return consultaRepository.pedidosEmAndamentoComPeca(idPeca);
    }
}
