package br.com.buscapeca.buscapecaapi.carrinho.service;

import br.com.buscapeca.buscapecaapi.carrinho.dto.AdicionarItemRequest;
import br.com.buscapeca.buscapecaapi.carrinho.dto.AlterarQuantidadeRequest;
import br.com.buscapeca.buscapecaapi.carrinho.dto.CarrinhoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.CarrinhoResponse.ItemCarrinhoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.CarrinhoResponse.LojaCarrinhoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.FreteRequest;
import br.com.buscapeca.buscapecaapi.carrinho.dto.FreteResponse;
import br.com.buscapeca.buscapecaapi.carrinho.dto.ItemAdicionadoResponse;
import br.com.buscapeca.buscapecaapi.carrinho.entity.Carrinho;
import br.com.buscapeca.buscapecaapi.carrinho.entity.ItemCarrinho;
import br.com.buscapeca.buscapecaapi.carrinho.mapper.CarrinhoMapper;
import br.com.buscapeca.buscapecaapi.carrinho.repository.CarrinhoRepository;
import br.com.buscapeca.buscapecaapi.carrinho.repository.ItemCarrinhoRepository;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.ConflitoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.integracao.frete.FreteService;
import br.com.buscapeca.buscapecaapi.integracao.geolocalizacao.GeolocalizacaoService;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaResumoResponse;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaParaCompra;
import br.com.buscapeca.buscapecaapi.peca.service.PecaCompraService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Carrinho com itens de varias lojas (BP1-22, 23, ADR-009). As pecas vem do
 * PecaCompraService e as lojas do LojaService: este servico nunca usa os repositorios
 * de outros modulos (ADR-003).
 *
 * O carrinho guarda so a peca e a quantidade. Preco, estoque e se o anuncio ainda esta
 * no ar sao lidos de novo a cada consulta, entao o comprador sempre ve o valor de agora;
 * o checkout (BP1-234) confere tudo outra vez antes de criar os pedidos.
 */
@Service
public class CarrinhoService {

    private static final Pattern CEP = Pattern.compile("^\\d{8}$");
    private static final String MSG_CEP_INVALIDO = "CEP inválido ou área sem cobertura de entrega";

    private final CarrinhoRepository carrinhoRepository;
    private final ItemCarrinhoRepository itemRepository;
    private final PecaCompraService pecaCompraService;
    private final LojaService lojaService;
    private final GeolocalizacaoService geolocalizacaoService;
    private final FreteService freteService;
    private final CarrinhoMapper mapper;

    public CarrinhoService(CarrinhoRepository carrinhoRepository, ItemCarrinhoRepository itemRepository,
                           PecaCompraService pecaCompraService, LojaService lojaService,
                           GeolocalizacaoService geolocalizacaoService, FreteService freteService,
                           CarrinhoMapper mapper) {
        this.carrinhoRepository = carrinhoRepository;
        this.itemRepository = itemRepository;
        this.pecaCompraService = pecaCompraService;
        this.lojaService = lojaService;
        this.geolocalizacaoService = geolocalizacaoService;
        this.freteService = freteService;
        this.mapper = mapper;
    }

    /** Itens agrupados por loja. Quem nunca adicionou nada recebe o carrinho vazio. */
    @Transactional(readOnly = true)
    public CarrinhoResponse obter(Long idUsuario) {
        return carrinhoRepository.findByIdUsuario(idUsuario)
                .map(c -> montar(itemRepository.findByIdCarrinhoOrderByIdAsc(c.getId())))
                .orElseGet(mapper::vazio);
    }

    /**
     * Soma a quantidade na linha da peca (ou cria a linha). O carrinho e criado no
     * primeiro item. 409 ESTOQUE_INSUFICIENTE se a soma passar do estoque da loja.
     */
    @Transactional
    public ItemAdicionadoResponse adicionar(Long idUsuario, AdicionarItemRequest req) {
        PecaParaCompra peca = pecaParaComprar(req.idPeca());
        Carrinho carrinho = carrinhoRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> carrinhoRepository.save(new Carrinho(idUsuario)));

        List<ItemCarrinho> itens = itemRepository.findByIdCarrinhoOrderByIdAsc(carrinho.getId());
        ItemCarrinho item = itens.stream()
                .filter(i -> i.getIdPeca().equals(peca.idPeca()))
                .findFirst()
                .orElseGet(() -> new ItemCarrinho(carrinho.getId(), peca.idPeca(), 0));
        int jaNoCarrinho = item.getQuantidade();
        int nova = jaNoCarrinho + req.quantidadeOuUm();
        if (nova > peca.quantidadeEstoque()) {
            throw new ConflitoException("ESTOQUE_INSUFICIENTE", String.format(
                    "Estoque insuficiente. A loja tem %d em estoque e você já tem %d no carrinho.",
                    Math.max(peca.quantidadeEstoque(), 0), jaNoCarrinho));
        }
        item.setQuantidade(nova);
        itemRepository.save(item);

        int outros = itens.stream()
                .filter(i -> !i.getIdPeca().equals(peca.idPeca()))
                .mapToInt(ItemCarrinho::getQuantidade)
                .sum();
        return new ItemAdicionadoResponse(peca.idPeca(), nova, outros + nova);
    }

    /** Troca a quantidade de um item. Devolve o carrinho inteiro, ja recalculado. */
    @Transactional
    public CarrinhoResponse alterarQuantidade(Long idUsuario, Long idItem, AlterarQuantidadeRequest req) {
        Carrinho carrinho = carrinhoDoUsuario(idUsuario);
        ItemCarrinho item = itemDoCarrinho(carrinho, idItem);
        PecaParaCompra peca = pecaParaComprar(item.getIdPeca());
        if (req.quantidade() > peca.quantidadeEstoque()) {
            throw new ConflitoException("ESTOQUE_INSUFICIENTE", String.format(
                    "Quantidade indisponível no estoque no momento. A loja tem %d.",
                    Math.max(peca.quantidadeEstoque(), 0)));
        }
        item.setQuantidade(req.quantidade());
        itemRepository.save(item);
        return montar(itemRepository.findByIdCarrinhoOrderByIdAsc(carrinho.getId()));
    }

    /** Tira o item do carrinho. 404 se o item nao e deste usuario. */
    @Transactional
    public void remover(Long idUsuario, Long idItem) {
        Carrinho carrinho = carrinhoDoUsuario(idUsuario);
        itemRepository.delete(itemDoCarrinho(carrinho, idItem));
    }

    /** Esvazia o carrinho. Sem carrinho, nao faz nada. */
    @Transactional
    public void esvaziar(Long idUsuario) {
        carrinhoRepository.findByIdUsuario(idUsuario)
                .ifPresent(c -> itemRepository.apagarTodos(c.getId()));
    }

    /**
     * Pecas e quantidades do carrinho (idPeca -> quantidade), na ordem em que entraram.
     * Usado pelo checkout (BP1-24), que confere preco e estoque de novo com as linhas
     * travadas. Sem carrinho, o mapa vem vazio.
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> itensParaCompra(Long idUsuario) {
        Map<Long, Integer> itens = new LinkedHashMap<>();
        carrinhoRepository.findByIdUsuario(idUsuario)
                .ifPresent(c -> itemRepository.findByIdCarrinhoOrderByIdAsc(c.getId())
                        .forEach(i -> itens.put(i.getIdPeca(), i.getQuantidade())));
        return itens;
    }

    /**
     * Frete e prazo por loja do carrinho para o CEP (BP1-23). Loja sem cotacao fica com o
     * frete a combinar (ADR-012). Se a consulta de CEP falhar, a regiao volta vazia e o
     * calculo segue: o front mostra o CEP no lugar da cidade.
     */
    @Transactional(readOnly = true)
    public FreteResponse calcularFrete(Long idUsuario, FreteRequest req) {
        String cep = req.cep().replaceAll("\\D", "");
        if (!CEP.matcher(cep).matches() || cep.chars().allMatch(c -> c == '0')) {
            throw new DadosInvalidosException("cep", MSG_CEP_INVALIDO);
        }
        FreteResponse.Regiao regiao = geolocalizacaoService.buscarEndereco(cep)
                .map(e -> new FreteResponse.Regiao(e.cidade(), e.estado()))
                .orElse(new FreteResponse.Regiao("", ""));

        List<FreteResponse.LojaFrete> lojas = new ArrayList<>();
        for (LojaCarrinhoResponse grupo : obter(idUsuario).lojas()) {
            var loja = grupo.loja();
            List<FreteResponse.OpcaoFrete> opcoes = freteService.cotar(loja.idLoja(), cep, grupo.subtotal())
                    .orElse(List.of())
                    .stream()
                    .map(o -> new FreteResponse.OpcaoFrete(o.tipo(), o.valor(), o.prazoDias()))
                    .toList();
            lojas.add(new FreteResponse.LojaFrete(loja.idLoja(), loja.nomeLoja(), loja.retiradaBalcao(),
                    opcoes.isEmpty(), opcoes));
        }
        return new FreteResponse(regiao, lojas);
    }

    // ----- Montagem e conferencias -----

    /** Agrupa os itens por loja, na ordem em que cada loja entrou no carrinho. */
    private CarrinhoResponse montar(List<ItemCarrinho> itens) {
        if (itens.isEmpty()) {
            return mapper.vazio();
        }
        Set<Long> idsPeca = new LinkedHashSet<>();
        itens.forEach(i -> idsPeca.add(i.getIdPeca()));
        Map<Long, PecaParaCompra> pecas = pecaCompraService.buscarVarias(idsPeca);

        Set<Long> idsLoja = new LinkedHashSet<>();
        pecas.values().forEach(p -> idsLoja.add(p.idLoja()));
        Map<Long, LojaResumoResponse> lojas = lojaService.resumos(idsLoja);

        Map<Long, List<ItemCarrinhoResponse>> porLoja = new LinkedHashMap<>();
        for (ItemCarrinho item : itens) {
            PecaParaCompra peca = pecas.get(item.getIdPeca());
            if (peca == null) {
                continue; // a peca nunca e apagada de verdade; so por seguranca
            }
            LojaResumoResponse loja = lojas.get(peca.idLoja());
            boolean lojaAtiva = loja != null && loja.ativa();
            porLoja.computeIfAbsent(peca.idLoja(), id -> new ArrayList<>())
                    .add(mapper.item(item, peca, lojaAtiva));
        }

        List<LojaCarrinhoResponse> grupos = new ArrayList<>();
        porLoja.forEach((idLoja, itensDaLoja) -> grupos.add(mapper.loja(idLoja, lojas.get(idLoja), itensDaLoja)));
        return mapper.carrinho(grupos);
    }

    /** Anuncio ATIVO de loja ativa; senao 404 com a mensagem da tela. */
    private PecaParaCompra pecaParaComprar(Long idPeca) {
        PecaParaCompra peca = pecaCompraService.buscar(idPeca)
                .filter(PecaParaCompra::disponivel)
                .orElseThrow(CarrinhoService::anuncioIndisponivel);
        boolean lojaAtiva = Optional.ofNullable(lojaService.resumos(Set.of(peca.idLoja())).get(peca.idLoja()))
                .map(LojaResumoResponse::ativa)
                .orElse(false);
        if (!lojaAtiva) {
            throw anuncioIndisponivel();
        }
        return peca;
    }

    private Carrinho carrinhoDoUsuario(Long idUsuario) {
        return carrinhoRepository.findByIdUsuario(idUsuario)
                .orElseThrow(CarrinhoService::itemNaoEncontrado);
    }

    private ItemCarrinho itemDoCarrinho(Carrinho carrinho, Long idItem) {
        return itemRepository.findByIdAndIdCarrinho(idItem, carrinho.getId())
                .orElseThrow(CarrinhoService::itemNaoEncontrado);
    }

    private static RecursoNaoEncontradoException anuncioIndisponivel() {
        return new RecursoNaoEncontradoException("Este anúncio não está mais disponível.");
    }

    private static RecursoNaoEncontradoException itemNaoEncontrado() {
        return new RecursoNaoEncontradoException("Este item não está mais no carrinho.");
    }
}
