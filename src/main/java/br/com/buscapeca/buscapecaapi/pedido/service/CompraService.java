package br.com.buscapeca.buscapecaapi.pedido.service;

import br.com.buscapeca.buscapecaapi.pedido.dto.CompraResponse;
import br.com.buscapeca.buscapecaapi.pedido.dto.FinalizarCompraRequest;
import br.com.buscapeca.buscapecaapi.pedido.dto.FinalizarCompraRequest.EscolhaLoja;
import br.com.buscapeca.buscapecaapi.pedido.entity.Compra;
import br.com.buscapeca.buscapecaapi.pedido.entity.ItemPedido;
import br.com.buscapeca.buscapecaapi.pedido.entity.Pedido;
import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;
import br.com.buscapeca.buscapecaapi.pedido.mapper.CompraMapper;
import br.com.buscapeca.buscapecaapi.pedido.repository.CompraRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.ItemPedidoRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.PedidoRepository;
import br.com.buscapeca.buscapecaapi.carrinho.service.CarrinhoService;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.ConflitoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaResumoResponse;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import br.com.buscapeca.buscapecaapi.peca.service.PecaCompraService;
import br.com.buscapeca.buscapecaapi.usuario.service.EnderecoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checkout (BP1-24, ADR-009): transforma o carrinho em uma compra com um pedido por loja.
 * Tudo numa transacao so (Documentacao do Back-end, secao 3): cria a compra, os pedidos
 * e os itens com o preco do momento, baixa o estoque com as linhas das pecas travadas e
 * esvazia o carrinho. Qualquer conferencia que falhe desfaz tudo.
 *
 * Carrinho, pecas, lojas e enderecos vem dos servicos desses modulos (ADR-003). Nenhum
 * valor e cobrado: o pagamento e combinado com cada loja (ADR-008).
 */
@Service
public class CompraService {

    private static final int TENTATIVAS_CODIGO = 10;

    private final CompraRepository compraRepository;
    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final CarrinhoService carrinhoService;
    private final PecaCompraService pecaCompraService;
    private final LojaService lojaService;
    private final EnderecoService enderecoService;
    private final GeradorCodigoRetirada geradorCodigo;
    private final CompraMapper mapper;

    public CompraService(CompraRepository compraRepository, PedidoRepository pedidoRepository,
                         ItemPedidoRepository itemPedidoRepository, CarrinhoService carrinhoService,
                         PecaCompraService pecaCompraService, LojaService lojaService,
                         EnderecoService enderecoService, GeradorCodigoRetirada geradorCodigo,
                         CompraMapper mapper) {
        this.compraRepository = compraRepository;
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
        this.carrinhoService = carrinhoService;
        this.pecaCompraService = pecaCompraService;
        this.lojaService = lojaService;
        this.enderecoService = enderecoService;
        this.geradorCodigo = geradorCodigo;
        this.mapper = mapper;
    }

    /**
     * Finaliza a compra do carrinho de quem esta logado.
     *
     * 400: falta a ciencia do pagamento direto, falta o endereco da entrega ou a loja nao
     * oferece a modalidade escolhida. 404: endereco de outra pessoa. 409 (o front volta ao
     * carrinho): CARRINHO_VAZIO, CARRINHO_ALTERADO, ANUNCIO_INDISPONIVEL,
     * ESTOQUE_INSUFICIENTE ou PRECO_ALTERADO.
     */
    @Transactional
    public CompraResponse finalizar(Long idUsuario, FinalizarCompraRequest req) {
        Map<Long, EscolhaLoja> escolhas = escolhasPorLoja(req);
        boolean temEntrega = escolhas.values().stream().anyMatch(e -> e.modalidade() == ModalidadeEntrega.ENTREGA);
        if (temEntrega) {
            if (req.idEndereco() == null) {
                throw new DadosInvalidosException("idEndereco", "Escolha o endereço de entrega.");
            }
            enderecoService.buscar(idUsuario, req.idEndereco()); // 404 se nao for do comprador
        }

        Map<Long, Integer> carrinho = carrinhoService.itensParaCompra(idUsuario);
        if (carrinho.isEmpty()) {
            throw new ConflitoException("CARRINHO_VAZIO", "Seu carrinho está vazio.");
        }

        // A partir daqui as linhas das pecas ficam travadas ate o fim da transacao
        Map<Long, PecaParaCompra> pecas = pecaCompraService.travarParaCompra(carrinho.keySet());
        Map<Long, Map<Long, Integer>> porLoja = agruparPorLoja(carrinho, pecas);
        Map<Long, LojaResumoResponse> lojas = lojaService.resumos(porLoja.keySet());
        conferirItens(carrinho, pecas, lojas);

        if (!porLoja.keySet().equals(escolhas.keySet())) {
            throw new ConflitoException("CARRINHO_ALTERADO", "Seu carrinho mudou. Revise os pedidos antes de confirmar.");
        }
        escolhas.values().forEach(e -> conferirModalidade(e, lojas.get(e.idLoja())));

        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Map.Entry<Long, Integer> item : carrinho.entrySet()) {
            total = total.add(subtotal(pecas.get(item.getKey()).preco(), item.getValue()));
        }
        if (req.totalEsperado() != null && req.totalEsperado().compareTo(total) != 0) {
            throw new ConflitoException("PRECO_ALTERADO",
                    "O preço de uma ou mais peças mudou. Revise o carrinho antes de confirmar.");
        }

        LocalDateTime agora = LocalDateTime.now();
        Compra compra = new Compra();
        compra.setIdComprador(idUsuario);
        compra.setIdEnderecoEntrega(temEntrega ? req.idEndereco() : null);
        compra.setValorTotal(total);
        compra.setDataCompra(agora);
        compra = compraRepository.save(compra);

        List<Pedido> pedidos = new ArrayList<>();
        Map<Long, List<ItemPedido>> itensPorPedido = new HashMap<>();
        Set<String> codigosUsados = new HashSet<>();
        for (Map.Entry<Long, Map<Long, Integer>> grupo : porLoja.entrySet()) {
            EscolhaLoja escolha = escolhas.get(grupo.getKey());
            Pedido pedido = new Pedido();
            pedido.setIdCompra(compra.getId());
            pedido.setIdLoja(grupo.getKey());
            pedido.setModalidadeEntrega(escolha.modalidade());
            pedido.setObservacao(textoOuNulo(escolha.observacao()));
            pedido.setStatus(StatusPedido.AGUARDANDO_ACEITE);
            pedido.setCodigoRetirada(codigoRetiradaNovo(codigosUsados));
            pedido.setDataPedido(agora);
            BigDecimal valorItens = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            for (Map.Entry<Long, Integer> item : grupo.getValue().entrySet()) {
                valorItens = valorItens.add(subtotal(pecas.get(item.getKey()).preco(), item.getValue()));
            }
            pedido.setValorItens(valorItens);
            pedido = pedidoRepository.save(pedido);

            List<ItemPedido> itens = new ArrayList<>();
            for (Map.Entry<Long, Integer> item : grupo.getValue().entrySet()) {
                itens.add(new ItemPedido(pedido.getId(), item.getKey(), item.getValue(),
                        pecas.get(item.getKey()).preco()));
            }
            itensPorPedido.put(pedido.getId(), itemPedidoRepository.saveAll(itens));
            pedidos.add(pedido);
        }

        pecaCompraService.baixarEstoque(carrinho);
        carrinhoService.esvaziar(idUsuario);

        return mapper.paraResponse(compra, pedidos, itensPorPedido, titulos(pecas), nomes(lojas));
    }

    /** A compra com o pedido de cada loja (tela de confirmacao). So o comprador; senao 404. */
    @Transactional(readOnly = true)
    public CompraResponse obter(Long idUsuario, Long idCompra) {
        Compra compra = compraRepository.findByIdAndIdComprador(idCompra, idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Compra não encontrada."));
        List<Pedido> pedidos = pedidoRepository.findByIdCompraOrderByIdAsc(compra.getId());
        List<Long> idsPedido = pedidos.stream().map(Pedido::getId).toList();

        Map<Long, List<ItemPedido>> itensPorPedido = new HashMap<>();
        Set<Long> idsPeca = new LinkedHashSet<>();
        if (!idsPedido.isEmpty()) {
            for (ItemPedido item : itemPedidoRepository.findByIdPedidoInOrderByIdAsc(idsPedido)) {
                itensPorPedido.computeIfAbsent(item.getIdPedido(), id -> new ArrayList<>()).add(item);
                idsPeca.add(item.getIdPeca());
            }
        }
        Set<Long> idsLoja = new LinkedHashSet<>();
        pedidos.forEach(p -> idsLoja.add(p.getIdLoja()));

        return mapper.paraResponse(compra, pedidos, itensPorPedido,
                titulos(pecaCompraService.buscarVarias(idsPeca)), nomes(lojaService.resumos(idsLoja)));
    }

    // ----- Conferencias -----

    /** Uma escolha por loja, todas com a ciencia do pagamento combinado com a loja (ADR-008). */
    private static Map<Long, EscolhaLoja> escolhasPorLoja(FinalizarCompraRequest req) {
        Map<Long, EscolhaLoja> escolhas = new LinkedHashMap<>();
        for (EscolhaLoja e : req.lojas()) {
            if (!e.cienciaPagamentoDireto()) {
                throw new DadosInvalidosException("lojas", "Confirme que leu as condições de cada loja.");
            }
            if (escolhas.put(e.idLoja(), e) != null) {
                throw new DadosInvalidosException("lojas", "Cada loja deve aparecer uma vez só.");
            }
        }
        return escolhas;
    }

    /** idLoja -> (idPeca -> quantidade), na ordem do carrinho. */
    private static Map<Long, Map<Long, Integer>> agruparPorLoja(Map<Long, Integer> carrinho,
                                                               Map<Long, PecaParaCompra> pecas) {
        Map<Long, Map<Long, Integer>> porLoja = new LinkedHashMap<>();
        carrinho.forEach((idPeca, quantidade) -> {
            PecaParaCompra peca = pecas.get(idPeca);
            if (peca != null) {
                porLoja.computeIfAbsent(peca.idLoja(), id -> new LinkedHashMap<>()).put(idPeca, quantidade);
            }
        });
        return porLoja;
    }

    /** Passo 6 do BP1-24: com as linhas travadas, confere de novo se tudo ainda pode ser comprado. */
    private static void conferirItens(Map<Long, Integer> carrinho, Map<Long, PecaParaCompra> pecas,
                                      Map<Long, LojaResumoResponse> lojas) {
        for (Map.Entry<Long, Integer> item : carrinho.entrySet()) {
            PecaParaCompra peca = pecas.get(item.getKey());
            LojaResumoResponse loja = peca == null ? null : lojas.get(peca.idLoja());
            if (peca == null || !peca.disponivel() || loja == null || !loja.ativa()) {
                String nome = peca == null ? "Uma peça do carrinho" : "\"" + peca.titulo() + "\"";
                throw new ConflitoException("ANUNCIO_INDISPONIVEL",
                        nome + " não está mais disponível. Remova do carrinho para continuar.");
            }
            if (item.getValue() > peca.quantidadeEstoque()) {
                throw new ConflitoException("ESTOQUE_INSUFICIENTE", String.format(
                        "O estoque de \"%s\" mudou. Ajuste a quantidade no carrinho.", peca.titulo()));
            }
        }
    }

    /** Mesmas opcoes que a tela oferece (opcoesRecebimento do checkout.js). */
    private static void conferirModalidade(EscolhaLoja escolha, LojaResumoResponse loja) {
        boolean entrega = loja.motoboyProprio() || loja.entregaCombinar();
        if (escolha.modalidade() == ModalidadeEntrega.RETIRADA && !loja.retiradaBalcao()) {
            throw new DadosInvalidosException("lojas",
                    "A loja " + loja.nomeLoja() + " não oferece retirada no balcão.");
        }
        // Loja sem nenhuma opcao marcada: a tela oferece entrega a combinar
        if (escolha.modalidade() == ModalidadeEntrega.ENTREGA && !entrega && loja.retiradaBalcao()) {
            throw new DadosInvalidosException("lojas",
                    "A loja " + loja.nomeLoja() + " não faz entrega. Escolha a retirada na loja.");
        }
    }

    // ----- Apoio -----

    private String codigoRetiradaNovo(Set<String> usadosNestaCompra) {
        for (int i = 0; i < TENTATIVAS_CODIGO; i++) {
            String codigo = geradorCodigo.gerar();
            if (!usadosNestaCompra.contains(codigo) && !pedidoRepository.existsByCodigoRetirada(codigo)) {
                usadosNestaCompra.add(codigo);
                return codigo;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código de retirada livre.");
    }

    private static BigDecimal subtotal(BigDecimal preco, int quantidade) {
        return preco.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_UP);
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static Map<Long, String> titulos(Map<Long, PecaParaCompra> pecas) {
        Map<Long, String> titulos = new HashMap<>();
        pecas.forEach((id, p) -> titulos.put(id, p.titulo()));
        return titulos;
    }

    private static Map<Long, String> nomes(Map<Long, LojaResumoResponse> lojas) {
        Map<Long, String> nomes = new HashMap<>();
        lojas.forEach((id, l) -> nomes.put(id, l.nomeLoja()));
        return nomes;
    }
}
