package br.com.buscapeca.buscapecaapi.pedido.service;

import br.com.buscapeca.buscapecaapi.pedido.dto.ConfirmarRetiradaRequest;
import br.com.buscapeca.buscapecaapi.pedido.dto.EnviarPedidoRequest;
import br.com.buscapeca.buscapecaapi.pedido.dto.MotivoRequest;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoDetalheResponse;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoDetalheResponse.Comprador;
import br.com.buscapeca.buscapecaapi.pedido.dto.PedidoRecebidoResponse;
import br.com.buscapeca.buscapecaapi.pedido.entity.Compra;
import br.com.buscapeca.buscapecaapi.pedido.entity.Entrega;
import br.com.buscapeca.buscapecaapi.pedido.entity.ItemPedido;
import br.com.buscapeca.buscapecaapi.pedido.entity.Pedido;
import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.ModalidadeEnvio;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusEntrega;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;
import br.com.buscapeca.buscapecaapi.pedido.mapper.PedidoMapper;
import br.com.buscapeca.buscapecaapi.pedido.repository.CompraRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.EntregaRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.ItemPedidoRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.PedidoConsultaRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.PedidoRepository;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.AcessoNegadoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.ConflitoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.NaoProcessavelException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.compartilhado.paginacao.PaginaResposta;
import br.com.buscapeca.buscapecaapi.compartilhado.paginacao.Paginacao;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaResumoResponse;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import br.com.buscapeca.buscapecaapi.peca.service.PecaCompraService;
import br.com.buscapeca.buscapecaapi.usuario.dto.EnderecoResponse;
import br.com.buscapeca.buscapecaapi.usuario.dto.UsuarioResponse;
import br.com.buscapeca.buscapecaapi.usuario.service.EnderecoService;
import br.com.buscapeca.buscapecaapi.usuario.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Ciclo de vida do pedido depois do checkout (Documentacao do Back-end, secao 5.8; ADR-009):
 * AGUARDANDO_ACEITE -> ACEITO ou RECUSADO -> EM_PREPARACAO -> PRONTO_RETIRADA ou EM_ENTREGA
 * -> CONCLUIDO. O comprador pode cancelar enquanto aguarda o aceite.
 *
 * Quem pode o que: a loja do pedido aceita, recusa, prepara, envia e confere a retirada; o
 * comprador cancela; os dois concluem a entrega. Pedido de outra pessoa vira 404, para nao
 * revelar que existe. Mudanca fora de ordem: 409 STATUS_INVALIDO. Toda mudanca trava a linha
 * do pedido e devolve o pedido atualizado.
 *
 * Pecas, lojas, usuarios e enderecos vem dos servicos desses modulos (ADR-003).
 */
@Service
public class PedidoService {

    private static final Sort MAIS_RECENTES = Sort.by(Sort.Direction.DESC, "dataPedido").and(Sort.by(Sort.Direction.DESC, "id"));

    private final PedidoConsultaRepository consultaRepository;
    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemRepository;
    private final CompraRepository compraRepository;
    private final EntregaRepository entregaRepository;
    private final PecaCompraService pecaCompraService;
    private final LojaService lojaService;
    private final UsuarioService usuarioService;
    private final EnderecoService enderecoService;
    private final PedidoMapper mapper;

    public PedidoService(PedidoConsultaRepository consultaRepository, PedidoRepository pedidoRepository,
                         ItemPedidoRepository itemRepository, CompraRepository compraRepository,
                         EntregaRepository entregaRepository, PecaCompraService pecaCompraService,
                         LojaService lojaService, UsuarioService usuarioService, EnderecoService enderecoService,
                         PedidoMapper mapper) {
        this.consultaRepository = consultaRepository;
        this.pedidoRepository = pedidoRepository;
        this.itemRepository = itemRepository;
        this.compraRepository = compraRepository;
        this.entregaRepository = entregaRepository;
        this.pecaCompraService = pecaCompraService;
        this.lojaService = lojaService;
        this.usuarioService = usuarioService;
        this.enderecoService = enderecoService;
        this.mapper = mapper;
    }

    /**
     * Pedidos aguardando aceite, aceitos, em preparacao, prontos para retirada ou em entrega
     * que tem a peca. Usado para impedir a exclusao do anuncio (BP1-227).
     */
    @Transactional(readOnly = true)
    public List<Long> pedidosEmAndamentoComPeca(Long idPeca) {
        return consultaRepository.pedidosEmAndamentoComPeca(idPeca);
    }

    // ----- Consultas -----

    /**
     * Pedidos recebidos pela loja de quem esta logado, do mais recente para o mais antigo
     * (BP1-26). A pagina comeca em 1, como o painel.js envia. 403 se a conta nao tem loja.
     */
    @Transactional(readOnly = true)
    public PaginaResposta<PedidoRecebidoResponse> listarRecebidos(Long idUsuario, String status, Integer pagina,
                                                                  Integer tamanho) {
        Long idLoja = lojaService.idDaLoja(idUsuario)
                .orElseThrow(() -> new AcessoNegadoException("Crie sua loja para receber pedidos."));
        StatusPedido filtro = lerStatus(status);
        int numero = pagina == null || pagina < 1 ? 1 : pagina;
        var pageable = Paginacao.de(numero - 1, tamanho, MAIS_RECENTES);
        Page<Pedido> pedidos = filtro == null
                ? pedidoRepository.findByIdLoja(idLoja, pageable)
                : pedidoRepository.findByIdLojaAndStatus(idLoja, filtro, pageable);

        List<Long> idsPedido = pedidos.getContent().stream().map(Pedido::getId).toList();
        Map<Long, List<ItemPedido>> itensPorPedido = itensPorPedido(idsPedido);
        Set<Long> idsPeca = new LinkedHashSet<>();
        itensPorPedido.values().forEach(l -> l.forEach(i -> idsPeca.add(i.getIdPeca())));
        Map<Long, PecaParaCompra> pecas = pecaCompraService.buscarVarias(idsPeca);
        Map<Long, String> compradores = nomesDosCompradores(pedidos.getContent());

        List<PedidoRecebidoResponse> itens = pedidos.getContent().stream()
                .map(p -> mapper.recebido(p, compradores.get(p.getIdCompra()),
                        mapper.resumoItens(itensPorPedido.getOrDefault(p.getId(), List.of()), pecas)))
                .toList();
        return new PaginaResposta<>(itens, numero, pedidos.getSize(), pedidos.getTotalElements(),
                pedidos.getTotalPages());
    }

    /** Detalhe para o comprador ou a loja do pedido (BP1-27). Qualquer outra pessoa: 404. */
    @Transactional(readOnly = true)
    public PedidoDetalheResponse detalhe(Long idUsuario, Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido).orElseThrow(PedidoService::naoEncontrado);
        Compra compra = compraDo(pedido);
        return montarDetalhe(pedido, compra, visao(idUsuario, pedido, compra));
    }

    // ----- Loja -----

    /** BP1-26: a loja aceita o pedido. */
    @Transactional
    public PedidoDetalheResponse aceitar(Long idUsuario, Long idPedido) {
        return mudar(idUsuario, idPedido, Quem.LOJA, EnumSet.of(StatusPedido.AGUARDANDO_ACEITE),
                "Este pedido não está mais aguardando aceite.", (p, c) -> p.setStatus(StatusPedido.ACEITO));
    }

    /** BP1-26: a loja recusa com motivo e o estoque volta (ADR-009). */
    @Transactional
    public PedidoDetalheResponse recusar(Long idUsuario, Long idPedido, MotivoRequest req) {
        return mudar(idUsuario, idPedido, Quem.LOJA, EnumSet.of(StatusPedido.AGUARDANDO_ACEITE),
                "Este pedido não está mais aguardando aceite.", (p, c) -> {
                    p.setStatus(StatusPedido.RECUSADO);
                    p.setMotivoRecusa(req.motivo().trim());
                    devolverEstoque(p);
                });
    }

    /** Fluxo C: a loja comeca a separar o pedido aceito. */
    @Transactional
    public PedidoDetalheResponse emPreparacao(Long idUsuario, Long idPedido) {
        return mudar(idUsuario, idPedido, Quem.LOJA, EnumSet.of(StatusPedido.ACEITO),
                "Só pedidos aceitos podem ir para preparação.", (p, c) -> p.setStatus(StatusPedido.EM_PREPARACAO));
    }

    /** BP1-29: pedido de retirada separado e esperando no balcao. */
    @Transactional
    public PedidoDetalheResponse prontoRetirada(Long idUsuario, Long idPedido) {
        return mudar(idUsuario, idPedido, Quem.LOJA, EnumSet.of(StatusPedido.ACEITO, StatusPedido.EM_PREPARACAO),
                "O pedido precisa estar aceito ou em preparação.", (p, c) -> {
                    exigirModalidade(p, ModalidadeEntrega.RETIRADA, "Este pedido é de entrega. Registre o envio.");
                    p.setStatus(StatusPedido.PRONTO_RETIRADA);
                });
    }

    /**
     * BP1-29: a loja digita o codigo que o comprador mostra no balcao e o pedido e concluido.
     * Codigo errado: 422 CODIGO_RETIRADA_INVALIDO, e nada muda.
     */
    @Transactional
    public PedidoDetalheResponse confirmarRetirada(Long idUsuario, Long idPedido, ConfirmarRetiradaRequest req) {
        return mudar(idUsuario, idPedido, Quem.LOJA, EnumSet.of(StatusPedido.PRONTO_RETIRADA),
                "O pedido ainda não está pronto para retirada.", (p, c) -> {
                    if (!normalizarCodigo(p.getCodigoRetirada()).equals(normalizarCodigo(req.codigo()))) {
                        throw new NaoProcessavelException("CODIGO_RETIRADA_INVALIDO", "Código de retirada não confere.");
                    }
                    p.setStatus(StatusPedido.CONCLUIDO);
                });
    }

    /** BP1-33: a loja registra o envio de um pedido de entrega. */
    @Transactional
    public PedidoDetalheResponse enviar(Long idUsuario, Long idPedido, EnviarPedidoRequest req) {
        if (req.modalidade() == ModalidadeEnvio.TRANSPORTADORA
                && (req.transportadora() == null || req.transportadora().isBlank())) {
            throw new DadosInvalidosException("transportadora", "Informe a transportadora.");
        }
        return mudar(idUsuario, idPedido, Quem.LOJA, EnumSet.of(StatusPedido.ACEITO, StatusPedido.EM_PREPARACAO),
                "O pedido precisa estar aceito ou em preparação.", (p, c) -> {
                    exigirModalidade(p, ModalidadeEntrega.ENTREGA, "Este pedido é de retirada no balcão.");
                    Entrega entrega = entregaRepository.findByIdPedido(p.getId()).orElseGet(Entrega::new);
                    entrega.setIdPedido(p.getId());
                    entrega.setModalidade(req.modalidade());
                    entrega.setTransportadora(textoOuNulo(req.transportadora()));
                    entrega.setCodigoRastreio(textoOuNulo(req.codigoRastreio()));
                    entrega.setStatusEntrega(StatusEntrega.EM_TRANSITO);
                    entrega.setAtualizadaEm(LocalDateTime.now());
                    entregaRepository.save(entrega);
                    p.setStatus(StatusPedido.EM_ENTREGA);
                });
    }

    // ----- Comprador e loja -----

    /** BP1-27: a entrega chegou. Pode ser confirmada pelo comprador ou pela loja. */
    @Transactional
    public PedidoDetalheResponse concluir(Long idUsuario, Long idPedido) {
        return mudar(idUsuario, idPedido, Quem.QUALQUER, EnumSet.of(StatusPedido.EM_ENTREGA),
                "Só pedidos em entrega podem ser concluídos.", (p, c) -> {
                    entregaRepository.findByIdPedido(p.getId()).ifPresent(e -> {
                        e.setStatusEntrega(StatusEntrega.ENTREGUE);
                        e.setAtualizadaEm(LocalDateTime.now());
                        entregaRepository.save(e);
                    });
                    p.setStatus(StatusPedido.CONCLUIDO);
                });
    }

    /** BP1-30: o comprador cancela enquanto a loja nao aceitou. O estoque volta (ADR-009). */
    @Transactional
    public PedidoDetalheResponse cancelar(Long idUsuario, Long idPedido, MotivoRequest req) {
        return mudar(idUsuario, idPedido, Quem.COMPRADOR, EnumSet.of(StatusPedido.AGUARDANDO_ACEITE),
                "A loja já aceitou este pedido. Converse com a loja para combinar o cancelamento.", (p, c) -> {
                    p.setStatus(StatusPedido.CANCELADO);
                    p.setMotivoCancelamento(req.motivo().trim());
                    devolverEstoque(p);
                });
    }

    // ----- Mecanica das mudancas -----

    private enum Quem { LOJA, COMPRADOR, QUALQUER }

    @FunctionalInterface
    private interface Mudanca {
        void aplicar(Pedido pedido, Compra compra);
    }

    /**
     * Trava o pedido, confere quem esta pedindo e o status de agora, aplica a mudanca e
     * devolve o pedido atualizado, na visao de quem pediu.
     */
    private PedidoDetalheResponse mudar(Long idUsuario, Long idPedido, Quem quem, Set<StatusPedido> permitidos,
                                        String mensagemStatus, Mudanca mudanca) {
        Pedido pedido = pedidoRepository.travar(idPedido).orElseThrow(PedidoService::naoEncontrado);
        Compra compra = compraDo(pedido);
        String visao = visao(idUsuario, pedido, compra);
        if (quem == Quem.LOJA && !PedidoMapper.VISAO_VENDEDOR.equals(visao)) {
            throw new AcessoNegadoException("Só a loja do pedido pode fazer isso.");
        }
        if (quem == Quem.COMPRADOR && !PedidoMapper.VISAO_COMPRADOR.equals(visao)) {
            throw new AcessoNegadoException("Só quem fez o pedido pode cancelá-lo.");
        }
        if (!permitidos.contains(pedido.getStatus())) {
            throw new ConflitoException("STATUS_INVALIDO", mensagemStatus);
        }
        mudanca.aplicar(pedido, compra);
        pedido = pedidoRepository.saveAndFlush(pedido); // flush: le de volta o atualizado_em do MySQL
        return montarDetalhe(pedido, compra, visao);
    }

    /** COMPRADOR, VENDEDOR ou 404. Quem compra da propria loja ve como comprador. */
    private String visao(Long idUsuario, Pedido pedido, Compra compra) {
        if (compra.getIdComprador().equals(idUsuario)) {
            return PedidoMapper.VISAO_COMPRADOR;
        }
        Optional<Long> minhaLoja = lojaService.idDaLoja(idUsuario);
        if (minhaLoja.isPresent() && minhaLoja.get().equals(pedido.getIdLoja())) {
            return PedidoMapper.VISAO_VENDEDOR;
        }
        throw naoEncontrado();
    }

    private void devolverEstoque(Pedido pedido) {
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        itemRepository.findByIdPedidoOrderByIdAsc(pedido.getId())
                .forEach(i -> quantidades.merge(i.getIdPeca(), i.getQuantidade(), Integer::sum));
        pecaCompraService.devolverEstoque(quantidades);
    }

    private static void exigirModalidade(Pedido pedido, ModalidadeEntrega esperada, String mensagem) {
        if (pedido.getModalidadeEntrega() != esperada) {
            throw new ConflitoException("MODALIDADE_INVALIDA", mensagem);
        }
    }

    // ----- Montagem -----

    private PedidoDetalheResponse montarDetalhe(Pedido pedido, Compra compra, String visao) {
        List<ItemPedido> itens = itemRepository.findByIdPedidoOrderByIdAsc(pedido.getId());
        Set<Long> idsPeca = new LinkedHashSet<>();
        itens.forEach(i -> idsPeca.add(i.getIdPeca()));
        Map<Long, PecaParaCompra> pecas = pecaCompraService.buscarVarias(idsPeca);

        LojaResumoResponse loja = lojaService.resumos(Set.of(pedido.getIdLoja())).get(pedido.getIdLoja());
        EnderecoResponse enderecoEntrega = pedido.getModalidadeEntrega() == ModalidadeEntrega.ENTREGA
                ? enderecoDaEntrega(compra)
                : null;
        Entrega entrega = entregaRepository.findByIdPedido(pedido.getId()).orElse(null);

        return mapper.detalhe(pedido, visao, itens, pecas, comprador(compra, enderecoEntrega),
                loja == null ? null : loja.nomeLoja(), enderecoEntrega, entrega);
    }

    /** O endereco pode ter sido apagado depois da compra: o pedido continua abrindo, sem ele. */
    private EnderecoResponse enderecoDaEntrega(Compra compra) {
        if (compra.getIdEnderecoEntrega() == null) {
            return null;
        }
        try {
            return enderecoService.buscarPorId(compra.getIdEnderecoEntrega());
        } catch (RecursoNaoEncontradoException e) {
            return null;
        }
    }

    /** Nome e contato de quem comprou; cidade do endereco de entrega ou do principal. */
    private Comprador comprador(Compra compra, EnderecoResponse enderecoEntrega) {
        UsuarioResponse u = usuarioService.buscarPerfil(compra.getIdComprador());
        EnderecoResponse cidade = enderecoEntrega != null ? enderecoEntrega
                : enderecoService.listar(compra.getIdComprador()).stream()
                        .filter(EnderecoResponse::principal).findFirst().orElse(null);
        return new Comprador(
                nomeDeExibicao(u),
                cidade == null ? null : cidade.cidade(),
                cidade == null ? null : cidade.estado(),
                u.nome(),
                u.telefone());
    }

    /** idCompra -> nome do comprador, com uma consulta por comprador (a pagina e pequena). */
    private Map<Long, String> nomesDosCompradores(List<Pedido> pedidos) {
        Set<Long> idsCompra = new LinkedHashSet<>();
        pedidos.forEach(p -> idsCompra.add(p.getIdCompra()));
        Map<Long, String> porComprador = new HashMap<>();
        Map<Long, String> porCompra = new HashMap<>();
        for (Compra c : compraRepository.findAllById(idsCompra)) {
            String nome = porComprador.computeIfAbsent(c.getIdComprador(),
                    id -> nomeDeExibicao(usuarioService.buscarPerfil(id)));
            porCompra.put(c.getId(), nome);
        }
        return porCompra;
    }

    private Map<Long, List<ItemPedido>> itensPorPedido(List<Long> idsPedido) {
        Map<Long, List<ItemPedido>> mapa = new HashMap<>();
        if (!idsPedido.isEmpty()) {
            for (ItemPedido i : itemRepository.findByIdPedidoInOrderByIdAsc(idsPedido)) {
                mapa.computeIfAbsent(i.getIdPedido(), id -> new ArrayList<>()).add(i);
            }
        }
        return mapa;
    }

    private Compra compraDo(Pedido pedido) {
        return compraRepository.findById(pedido.getIdCompra()).orElseThrow(PedidoService::naoEncontrado);
    }

    // ----- Apoio -----

    private static StatusPedido lerStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return StatusPedido.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DadosInvalidosException("status", "Status de pedido inválido.");
        }
    }

    /** Sem hifen, espaco e maiusculas/minusculas: "hp 3318" confere com "HP-3318". */
    static String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static String nomeDeExibicao(UsuarioResponse u) {
        return u.nomeExibicao() != null && !u.nomeExibicao().isBlank() ? u.nomeExibicao() : u.nome();
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static RecursoNaoEncontradoException naoEncontrado() {
        return new RecursoNaoEncontradoException("Pedido não encontrado.");
    }
}
