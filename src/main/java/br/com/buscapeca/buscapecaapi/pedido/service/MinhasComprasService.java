package br.com.buscapeca.buscapecaapi.pedido.service;

import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.paginacao.Paginacao;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaResumoResponse;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import br.com.buscapeca.buscapecaapi.peca.service.PecaCompraService;
import br.com.buscapeca.buscapecaapi.pedido.dto.MinhasComprasResponse;
import br.com.buscapeca.buscapecaapi.pedido.dto.MinhasComprasResponse.CompraDoHistorico;
import br.com.buscapeca.buscapecaapi.pedido.dto.MinhasComprasResponse.PedidoDoHistorico;
import br.com.buscapeca.buscapecaapi.pedido.dto.MinhasComprasResponse.Resumo;
import br.com.buscapeca.buscapecaapi.pedido.entity.Compra;
import br.com.buscapeca.buscapecaapi.pedido.entity.ItemPedido;
import br.com.buscapeca.buscapecaapi.pedido.entity.Pedido;
import br.com.buscapeca.buscapecaapi.pedido.enums.StatusPedido;
import br.com.buscapeca.buscapecaapi.pedido.mapper.CompraMapper;
import br.com.buscapeca.buscapecaapi.pedido.mapper.PedidoMapper;
import br.com.buscapeca.buscapecaapi.pedido.repository.CompraRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.ItemPedidoRepository;
import br.com.buscapeca.buscapecaapi.pedido.repository.PedidoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Historico do comprador, "Meus pedidos" (GET /api/compras; BP1-20, BP1-27): uma compra por
 * linha com o pedido de cada loja, filtros e o resumo dos cartoes do topo.
 *
 * A busca "q" procura no codigo, no nome da loja e no titulo das pecas, que sao de outros
 * modulos (ADR-003): por isso o filtro e a pagina sao montados aqui, sobre as compras do
 * periodo, e nao no SQL. O historico de uma pessoa e pequeno; se crescer, a busca pode
 * virar uma consulta com os ids de loja e peca vindos dos servicos desses modulos.
 */
@Service
public class MinhasComprasService {

    /** Limites do DATETIME do MySQL, usados quando "de" ou "ate" nao vem. */
    private static final LocalDateTime INICIO_SEM_FILTRO = LocalDateTime.of(1000, 1, 1, 0, 0);
    private static final LocalDateTime FIM_SEM_FILTRO = LocalDateTime.of(9999, 12, 31, 0, 0);

    private static final Set<StatusPedido> ATENCAO = EnumSet.of(StatusPedido.PRONTO_RETIRADA, StatusPedido.EM_ENTREGA);
    private static final Set<StatusPedido> ANDAMENTO =
            EnumSet.of(StatusPedido.AGUARDANDO_ACEITE, StatusPedido.ACEITO, StatusPedido.EM_PREPARACAO);

    private final CompraRepository compraRepository;
    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final PecaCompraService pecaCompraService;
    private final LojaService lojaService;
    private final CompraMapper compraMapper;
    private final PedidoMapper pedidoMapper;

    public MinhasComprasService(CompraRepository compraRepository, PedidoRepository pedidoRepository,
                                ItemPedidoRepository itemPedidoRepository, PecaCompraService pecaCompraService,
                                LojaService lojaService, CompraMapper compraMapper, PedidoMapper pedidoMapper) {
        this.compraRepository = compraRepository;
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
        this.pecaCompraService = pecaCompraService;
        this.lojaService = lojaService;
        this.compraMapper = compraMapper;
        this.pedidoMapper = pedidoMapper;
    }

    /**
     * Compras de quem esta logado, da mais recente para a mais antiga. Uma compra aparece se
     * algum pedido dela passa nos filtros, e so com esses pedidos.
     *
     * @param q      codigo do pedido (BP-1042 ou 1042), nome da loja ou titulo da peca
     * @param status status do pedido
     * @param de     AAAA-MM-DD, inclusive
     * @param ate    AAAA-MM-DD, inclusive
     * @param pagina comeca em 0
     * @throws DadosInvalidosException status inexistente, data fora do formato ou "de" depois de "ate"
     */
    @Transactional(readOnly = true)
    public MinhasComprasResponse listar(Long idUsuario, String q, String status, String de, String ate,
                                        Integer pagina, Integer tamanho) {
        StatusPedido filtroStatus = lerStatus(status);
        LocalDate dataInicial = lerData("de", de);
        LocalDate dataFinal = lerData("ate", ate);
        if (dataInicial != null && dataFinal != null && dataInicial.isAfter(dataFinal)) {
            throw new DadosInvalidosException("ate", "A data final deve ser igual ou depois da inicial.");
        }
        Pageable pageable = Paginacao.de(pagina, tamanho, Sort.unsorted());

        List<Compra> compras = compraRepository
                .findByIdCompradorAndDataCompraGreaterThanEqualAndDataCompraLessThanOrderByDataCompraDescIdDesc(
                        idUsuario,
                        dataInicial == null ? INICIO_SEM_FILTRO : dataInicial.atStartOfDay(),
                        dataFinal == null ? FIM_SEM_FILTRO : dataFinal.plusDays(1).atStartOfDay());

        List<CompraDoHistorico> filtradas = compras.isEmpty() ? List.of()
                : montarEFiltrar(compras, filtroStatus, normalizar(q), soDigitos(q));

        int inicio = (int) Math.min(pageable.getOffset(), filtradas.size());
        int fim = Math.min(inicio + pageable.getPageSize(), filtradas.size());
        int totalPaginas = (int) Math.ceil(filtradas.size() / (double) pageable.getPageSize());
        return new MinhasComprasResponse(filtradas.subList(inicio, fim), pageable.getPageNumber(),
                pageable.getPageSize(), filtradas.size(), totalPaginas, resumo(idUsuario));
    }

    private List<CompraDoHistorico> montarEFiltrar(List<Compra> compras, StatusPedido status, String termo,
                                                   String digitos) {
        List<Long> idsCompra = compras.stream().map(Compra::getId).toList();
        Map<Long, List<Pedido>> pedidosPorCompra = new HashMap<>();
        Set<Long> idsLoja = new LinkedHashSet<>();
        List<Long> idsPedido = new ArrayList<>();
        for (Pedido p : pedidoRepository.findByIdCompraInOrderByIdAsc(idsCompra)) {
            pedidosPorCompra.computeIfAbsent(p.getIdCompra(), id -> new ArrayList<>()).add(p);
            idsLoja.add(p.getIdLoja());
            idsPedido.add(p.getId());
        }

        Map<Long, List<ItemPedido>> itensPorPedido = new HashMap<>();
        Set<Long> idsPeca = new LinkedHashSet<>();
        if (!idsPedido.isEmpty()) {
            for (ItemPedido item : itemPedidoRepository.findByIdPedidoInOrderByIdAsc(idsPedido)) {
                itensPorPedido.computeIfAbsent(item.getIdPedido(), id -> new ArrayList<>()).add(item);
                idsPeca.add(item.getIdPeca());
            }
        }
        Map<Long, PecaParaCompra> pecas = pecaCompraService.buscarVarias(idsPeca);
        Map<Long, String> nomesLoja = new HashMap<>();
        for (Map.Entry<Long, LojaResumoResponse> loja : lojaService.resumos(idsLoja).entrySet()) {
            nomesLoja.put(loja.getKey(), loja.getValue().nomeLoja());
        }

        List<CompraDoHistorico> resultado = new ArrayList<>();
        for (Compra compra : compras) {
            List<PedidoDoHistorico> pedidos = new ArrayList<>();
            for (Pedido p : pedidosPorCompra.getOrDefault(compra.getId(), List.of())) {
                List<ItemPedido> itens = itensPorPedido.getOrDefault(p.getId(), List.of());
                PedidoDoHistorico linha = compraMapper.pedidoDoHistorico(p, nomesLoja.get(p.getIdLoja()),
                        pedidoMapper.resumoItens(itens, pecas), itens, pecas);
                if ((status == null || p.getStatus() == status) && passaNaBusca(linha, termo, digitos)) {
                    pedidos.add(linha);
                }
            }
            if (!pedidos.isEmpty()) {
                resultado.add(compraMapper.compraDoHistorico(compra, pedidos));
            }
        }
        return resultado;
    }

    /** "1042" acha o BP-1042; o resto procura sem acento nem maiusculas no codigo, loja e pecas. */
    private static boolean passaNaBusca(PedidoDoHistorico p, String termo, String digitos) {
        if (termo.isEmpty()) {
            return true;
        }
        if (!digitos.isEmpty() && digitos.equals(soDigitos(p.codigo()))) {
            return true;
        }
        return Stream.concat(Stream.of(p.codigo(), p.nomeLoja(), p.resumoItens()),
                        p.itens().stream().map(MinhasComprasResponse.ItemDoHistorico::titulo))
                .anyMatch(texto -> normalizar(texto).contains(termo));
    }

    /** Todos os pedidos do comprador, sem os filtros da busca. */
    private Resumo resumo(Long idUsuario) {
        Map<StatusPedido, Long> porStatus = new EnumMap<>(StatusPedido.class);
        for (Object[] linha : pedidoRepository.contarPorStatusDoComprador(idUsuario)) {
            porStatus.put((StatusPedido) linha[0], ((Number) linha[1]).longValue());
        }
        return new Resumo(soma(porStatus, ATENCAO), soma(porStatus, ANDAMENTO),
                porStatus.getOrDefault(StatusPedido.PRONTO_RETIRADA, 0L));
    }

    private static long soma(Map<StatusPedido, Long> porStatus, Set<StatusPedido> grupo) {
        return grupo.stream().mapToLong(s -> porStatus.getOrDefault(s, 0L)).sum();
    }

    // ----- Leitura dos filtros -----

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

    private static LocalDate lerData(String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new DadosInvalidosException(campo, "Data inválida. Use o formato AAAA-MM-DD.");
        }
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private static String soDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }
}
