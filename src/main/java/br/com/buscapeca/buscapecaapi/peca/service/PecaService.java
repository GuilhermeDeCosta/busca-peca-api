package br.com.buscapeca.buscapecaapi.peca.service;

import br.com.buscapeca.buscapecaapi.administracao.service.AuditoriaService;
import br.com.buscapeca.buscapecaapi.catalogo.service.CatalogoService;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.AcessoNegadoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.ConflitoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.compartilhado.paginacao.PaginaResposta;
import br.com.buscapeca.buscapecaapi.compartilhado.paginacao.Paginacao;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.peca.dto.AplicacaoRequest;
import br.com.buscapeca.buscapecaapi.peca.dto.AplicacaoResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.CompatibilidadesRequest;
import br.com.buscapeca.buscapecaapi.peca.dto.CompatibilidadesResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.HistoricoPrecoResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.MeuAnuncioResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.MeusAnunciosFiltros;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaRequest;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.StatusAnuncioResponse;
import br.com.buscapeca.buscapecaapi.peca.entity.Compatibilidade;
import br.com.buscapeca.buscapecaapi.peca.entity.Peca;
import br.com.buscapeca.buscapecaapi.peca.enums.NivelEstoque;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;
import br.com.buscapeca.buscapecaapi.peca.mapper.PecaMapper;
import br.com.buscapeca.buscapecaapi.peca.repository.CompatibilidadeRepository;
import br.com.buscapeca.buscapecaapi.peca.repository.FiltroMeusAnuncios;
import br.com.buscapeca.buscapecaapi.peca.repository.PecaRepository;
import br.com.buscapeca.buscapecaapi.pedido.service.PedidoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Cadastro e edicao de pecas (BP1-3, 44, 45). A loja vem do LojaService e os veiculos do
 * CatalogoService: este servico nunca usa os repositorios de outros modulos (ADR-003).
 *
 * Rascunho so exige o titulo. Para publicar (ATIVO ou PAUSADO) a peca precisa de
 * categoria, codigo OEM e preco; a compatibilidade e as fotos chegam em chamadas
 * seguintes do form-peca (salvarAnuncio), por isso nao sao conferidas aqui.
 *
 * Ao criar e ao editar, o HistoricoPrecoService registra a publicacao e as mudancas de
 * preco (BP1-53), na mesma transacao do salvamento da peca.
 *
 * No painel Minha Loja, o vendedor lista, pausa, reativa e exclui os proprios anuncios
 * (BP1-225, 226, 227). Para excluir, o PedidoService diz se a peca esta em algum pedido
 * em andamento.
 */
@Service
public class PecaService {

    private static final Pattern OEM = Pattern.compile("^[A-Z0-9/]{3,50}$");
    private static final Pattern SKU = Pattern.compile("^[A-Z0-9][A-Z0-9._\\-/]{1,49}$");
    private static final Pattern SEPARADORES_OEM = Pattern.compile("[\\s.\\-]");
    private static final int FAIXA_MAXIMA_ANOS = 40;
    private static final int VEICULOS_MAXIMOS = 500;
    /** Mais novo primeiro; o id desempata e deixa a paginacao estavel. */
    private static final Sort ORDEM_MEUS_ANUNCIOS = Sort.by(Sort.Order.desc("id"));

    private final PecaRepository pecaRepository;
    private final CompatibilidadeRepository compatibilidadeRepository;
    private final LojaService lojaService;
    private final CatalogoService catalogoService;
    private final PecaMapper mapper;
    private final HistoricoPrecoService historicoPrecoService;
    private final PedidoService pedidoService;
    private final AuditoriaService auditoriaService;

    public PecaService(PecaRepository pecaRepository, CompatibilidadeRepository compatibilidadeRepository,
                       LojaService lojaService, CatalogoService catalogoService, PecaMapper mapper,
                       HistoricoPrecoService historicoPrecoService, PedidoService pedidoService,
                       AuditoriaService auditoriaService) {
        this.pecaRepository = pecaRepository;
        this.compatibilidadeRepository = compatibilidadeRepository;
        this.lojaService = lojaService;
        this.catalogoService = catalogoService;
        this.mapper = mapper;
        this.historicoPrecoService = historicoPrecoService;
        this.pedidoService = pedidoService;
        this.auditoriaService = auditoriaService;
    }

    /** Cria o anuncio na loja de quem esta logado, como rascunho ou ja publicado. */
    @Transactional
    public PecaResponse criar(Long idUsuario, PecaRequest req) {
        Peca peca = new Peca();
        peca.setIdLoja(lojaService.idDaLojaParaAnunciar(idUsuario));
        preencher(peca, req);
        peca = pecaRepository.saveAndFlush(peca);
        registrarPreco(idUsuario, peca);
        return mapper.paraResponse(peca, List.of());
    }

    /** Edita o anuncio inteiro. So a loja dona da peca. */
    @Transactional
    public PecaResponse atualizar(Long idUsuario, Long idPeca, PecaRequest req) {
        Peca peca = daMinhaLoja(idUsuario, idPeca);
        preencher(peca, req);
        peca = pecaRepository.saveAndFlush(peca);
        registrarPreco(idUsuario, peca);
        return mapper.paraResponse(peca, aplicacoes(idPeca));
    }

    /**
     * Detalhe da peca. Qualquer pessoa ve os anuncios ativos; o dono ve tambem
     * rascunho e pausado (e o que o form-peca carrega para editar).
     */
    @Transactional(readOnly = true)
    public PecaResponse obter(Long idPeca, Optional<Long> idUsuario) {
        Peca peca = pecaRepository.findByIdAndStatusNot(idPeca, StatusPeca.EXCLUIDO)
                .orElseThrow(PecaService::naoEncontrada);
        boolean dono = idUsuario.flatMap(lojaService::idDaLoja)
                .filter(idLoja -> idLoja.equals(peca.getIdLoja()))
                .isPresent();
        if (!dono && peca.getStatus() != StatusPeca.ATIVO) {
            throw naoEncontrada();
        }
        return mapper.paraResponse(peca, aplicacoes(idPeca));
    }

    /**
     * Troca a lista de veiculos compativeis pela recebida. Cada ano da faixa vira uma
     * linha em compatibilidade; o ano ou a versao que faltar no catalogo e criado.
     */
    @Transactional
    public CompatibilidadesResponse definirCompatibilidades(Long idUsuario, Long idPeca, CompatibilidadesRequest req) {
        Peca peca = daMinhaLoja(idUsuario, idPeca);
        if (peca.getStatus().publicado() && req.aplicacoes().isEmpty()) {
            throw new DadosInvalidosException("aplicacoes", "Adicione pelo menos uma aplicação verificada.");
        }
        Set<Long> idsVeiculo = new LinkedHashSet<>();
        for (AplicacaoRequest a : req.aplicacoes()) {
            int inicial = a.anoInicial();
            int fim = a.anoFinal() == null ? inicial : a.anoFinal();
            if (fim < inicial) {
                throw new DadosInvalidosException("aplicacoes", "O ano final não pode ser antes do ano inicial.");
            }
            if (fim - inicial > FAIXA_MAXIMA_ANOS) {
                throw new DadosInvalidosException("aplicacoes", "Use faixas de no máximo " + FAIXA_MAXIMA_ANOS + " anos.");
            }
            for (int ano = inicial; ano <= fim; ano++) {
                idsVeiculo.add(catalogoService.obterOuCriarVeiculo(a.marca(), a.modelo(), (short) ano, a.versao()));
            }
            if (idsVeiculo.size() > VEICULOS_MAXIMOS) {
                throw new DadosInvalidosException("aplicacoes", "A compatibilidade passou de " + VEICULOS_MAXIMOS
                        + " veículos. Junte as aplicações em menos faixas de ano.");
            }
        }
        compatibilidadeRepository.apagarDaPeca(idPeca);
        compatibilidadeRepository.saveAll(idsVeiculo.stream().map(id -> new Compatibilidade(idPeca, id)).toList());
        return new CompatibilidadesResponse(idPeca, aplicacoes(idPeca));
    }

    /**
     * Historico de preco do anuncio (BP1-53), da mudanca mais recente para a publicacao.
     * So a loja dona; vazio enquanto a peca nunca foi publicada.
     */
    @Transactional(readOnly = true)
    public List<HistoricoPrecoResponse> historicoPreco(Long idUsuario, Long idPeca) {
        daMinhaLoja(idUsuario, idPeca);
        return historicoPrecoService.listar(idPeca);
    }

    // ----- Gerenciar anuncios no painel Minha Loja (BP1-225, 226, 227) -----

    /**
     * Anuncios da loja de quem esta logado: ativos, pausados e rascunhos, do mais novo para
     * o mais antigo. A pagina comeca em 1, como o painel.js envia.
     */
    @Transactional(readOnly = true)
    public PaginaResposta<MeuAnuncioResponse> listarMinhas(Long idUsuario, MeusAnunciosFiltros f) {
        Long idLoja = lojaService.idDaLojaParaAnunciar(idUsuario);

        Map<String, String> erros = new LinkedHashMap<>();
        StatusPeca status = lerStatus(f.status(), erros);
        NivelEstoque estoque = lerEstoque(f.estoque(), erros);
        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }
        String termo = vazioParaNulo(f.q());
        String oem = normalizarOem(termo);

        int pagina = f.pagina() == null || f.pagina() < 1 ? 1 : f.pagina();
        Page<Peca> resultado = pecaRepository.findAll(
                FiltroMeusAnuncios.de(idLoja, status, estoque, termo, oem),
                Paginacao.de(pagina - 1, f.tamanho(), ORDEM_MEUS_ANUNCIOS));

        return new PaginaResposta<>(
                resultado.getContent().stream().map(mapper::paraMeuAnuncio).toList(),
                pagina,
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    /** Tira o anuncio ativo das buscas sem excluir (BP1-226). 409 STATUS_INVALIDO se nao estiver ativo. */
    @Transactional
    public StatusAnuncioResponse pausar(Long idUsuario, Long idPeca) {
        Peca peca = daMinhaLoja(idUsuario, idPeca);
        if (peca.getStatus() != StatusPeca.ATIVO) {
            throw new ConflitoException("STATUS_INVALIDO",
                    "Só anúncios ativos podem ser pausados. A lista foi atualizada.");
        }
        peca.setStatus(StatusPeca.PAUSADO);
        return mapper.paraStatus(pecaRepository.saveAndFlush(peca));
    }

    /**
     * Volta o anuncio pausado para as buscas (BP1-226). 409 STATUS_INVALIDO se nao estiver
     * pausado e 409 ESTOQUE_ZERADO se nao tiver unidades: anuncio ativo sem estoque so gera
     * pedido que a loja vai ter que recusar.
     */
    @Transactional
    public StatusAnuncioResponse reativar(Long idUsuario, Long idPeca) {
        Peca peca = daMinhaLoja(idUsuario, idPeca);
        if (peca.getStatus() != StatusPeca.PAUSADO) {
            throw new ConflitoException("STATUS_INVALIDO",
                    "Só anúncios pausados podem ser reativados. A lista foi atualizada.");
        }
        if (peca.getQuantidadeEstoque() <= 0) {
            throw new ConflitoException("ESTOQUE_ZERADO",
                    "Este anúncio está sem estoque. Atualize a quantidade antes de reativar.");
        }
        peca.setStatus(StatusPeca.ATIVO);
        return mapper.paraStatus(pecaRepository.saveAndFlush(peca));
    }

    /**
     * Exclusao logica do anuncio ou rascunho (BP1-227): status EXCLUIDO, a peca some da
     * loja e das buscas, e pedidos antigos continuam apontando para ela. 409
     * PEDIDOS_EM_ANDAMENTO se algum pedido ainda pode gerar entrega. Vai para log_auditoria.
     */
    @Transactional
    public void excluir(Long idUsuario, Long idPeca) {
        Peca peca = daMinhaLoja(idUsuario, idPeca);
        List<Long> emAndamento = pedidoService.pedidosEmAndamentoComPeca(idPeca);
        if (!emAndamento.isEmpty()) {
            throw new ConflitoException("PEDIDOS_EM_ANDAMENTO", mensagemPedidosEmAndamento(emAndamento));
        }
        StatusPeca anterior = peca.getStatus();
        peca.setStatus(StatusPeca.EXCLUIDO);
        pecaRepository.saveAndFlush(peca);
        auditoriaService.registrar(idUsuario, "EXCLUIR_PECA", "peca",
                Map.of("idPeca", idPeca, "statusAnterior", anterior.name()));
    }

    // ----- Regras internas -----

    /** Peca que ainda existe e e da loja de quem esta logado. */
    private Peca daMinhaLoja(Long idUsuario, Long idPeca) {
        Peca peca = pecaRepository.findByIdAndStatusNot(idPeca, StatusPeca.EXCLUIDO)
                .orElseThrow(PecaService::naoEncontrada);
        Long idLoja = lojaService.idDaLojaParaAnunciar(idUsuario);
        if (!peca.getIdLoja().equals(idLoja)) {
            throw new AcessoNegadoException("Este anúncio é de outra loja.");
        }
        return peca;
    }

    /**
     * Depois de salvar: grava a publicacao ou a mudanca de preco no historico. Rascunho
     * e preco repetido nao geram linha (a regra fica no HistoricoPrecoService).
     */
    private void registrarPreco(Long idUsuario, Peca peca) {
        historicoPrecoService.registrarSeMudou(idUsuario, peca.getId(), peca.getStatus().publicado(), peca.getPreco());
    }

    /** Confere e copia os dados do formulario. Junta todos os erros para a tela marcar de uma vez. */
    private void preencher(Peca peca, PecaRequest req) {
        if (req.status() == StatusPeca.EXCLUIDO) {
            throw new DadosInvalidosException("status", "Para excluir o anúncio, use o botão Excluir.");
        }
        String oem = normalizarOem(req.codigoOem());
        String ean = vazioParaNulo(req.codigoEan());
        String sku = req.codigoSku() == null ? null : vazioParaNulo(req.codigoSku().toUpperCase(Locale.ROOT));
        // O rascunho sem preco chega como 0: fica vazio no banco (ck_peca_preco exige > 0)
        BigDecimal preco = req.preco() == null || req.preco().signum() <= 0 ? null : req.preco();
        boolean publicar = req.status().publicado();

        Map<String, String> erros = new LinkedHashMap<>();
        if (req.idCategoria() != null && !catalogoService.categoriaExiste(req.idCategoria())) {
            erros.put("idCategoria", "Categoria não encontrada.");
        } else if (publicar && req.idCategoria() == null) {
            erros.put("idCategoria", "Escolha a categoria.");
        }
        if (oem != null && !OEM.matcher(oem).matches()) {
            erros.put("codigoOem", "OEM aceita só letras, números e barra, com 3 a 50 caracteres.");
        } else if (publicar && oem == null) {
            erros.put("codigoOem", "Informe o código OEM.");
        }
        if (ean != null && !eanValido(ean)) {
            erros.put("codigoEan", "EAN inválido: confira os números e o último dígito.");
        }
        if (sku != null && !SKU.matcher(sku).matches()) {
            erros.put("codigoSku", "SKU aceita letras, números, ponto, hífen, sublinhado e barra.");
        }
        if (publicar && preco == null) {
            erros.put("preco", "Informe um preço maior que zero.");
        }
        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }

        peca.setTitulo(req.titulo().trim());
        peca.setDescricao(vazioParaNulo(req.descricao()));
        peca.setIdCategoria(req.idCategoria());
        peca.setMarcaPeca(vazioParaNulo(req.marcaPeca()));
        peca.setCodigoOem(oem);
        peca.setCodigoEan(ean);
        peca.setCodigoSku(sku);
        peca.setCaracteristicas(vazioParaNulo(req.caracteristicas()));
        peca.setCondicao(req.condicao());
        peca.setOrigem(req.origem());
        peca.setPreco(preco);
        peca.setQuantidadeEstoque(req.quantidadeEstoque() == null ? 0 : req.quantidadeEstoque());
        peca.setProntaEntrega(!Boolean.FALSE.equals(req.prontaEntrega()));
        peca.setPrazoExpedicaoDias(req.prazoExpedicaoDias() == null ? 0 : req.prazoExpedicaoDias());
        peca.setStatus(req.status());
    }

    /** Filtro de status da listagem: so os que o painel mostra. Vazio = todos. */
    private static StatusPeca lerStatus(String valor, Map<String, String> erros) {
        String texto = vazioParaNulo(valor);
        if (texto == null) {
            return null;
        }
        return switch (texto.toUpperCase(Locale.ROOT)) {
            case "ATIVO" -> StatusPeca.ATIVO;
            case "PAUSADO" -> StatusPeca.PAUSADO;
            case "RASCUNHO" -> StatusPeca.RASCUNHO;
            default -> {
                erros.put("status", "Use ATIVO, PAUSADO ou RASCUNHO.");
                yield null;
            }
        };
    }

    /** Filtro de estoque da listagem. Vazio = todos. */
    private static NivelEstoque lerEstoque(String valor, Map<String, String> erros) {
        String texto = vazioParaNulo(valor);
        if (texto == null) {
            return null;
        }
        try {
            return NivelEstoque.valueOf(texto.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            erros.put("estoque", "Use ZERADO, BAIXO ou NORMAL.");
            return null;
        }
    }

    /** Ex.: "Esta peça tem um pedido em andamento (BP-2081). Conclua ou recuse...". */
    static String mensagemPedidosEmAndamento(List<Long> idsPedido) {
        String codigos = String.join(", ", idsPedido.stream().map(id -> "BP-" + id).toList());
        String quantos = idsPedido.size() == 1 ? "um pedido" : idsPedido.size() + " pedidos";
        return "Esta peça tem " + quantos + " em andamento (" + codigos + "). "
                + "Conclua ou recuse antes de excluir, ou pause o anúncio.";
    }

    private List<AplicacaoResponse> aplicacoes(Long idPeca) {
        List<Long> idsVeiculo = compatibilidadeRepository.findByIdPeca(idPeca).stream()
                .map(Compatibilidade::getIdVeiculo)
                .toList();
        if (idsVeiculo.isEmpty()) {
            return List.of();
        }
        return mapper.agruparEmFaixas(catalogoService.veiculosPorIds(idsVeiculo));
    }

    /** Mesmo formato do form-peca: maiusculas, sem espacos, pontos ou hifens. */
    private static String normalizarOem(String valor) {
        if (valor == null) {
            return null;
        }
        return vazioParaNulo(SEPARADORES_OEM.matcher(valor.toUpperCase(Locale.ROOT)).replaceAll(""));
    }

    /** GTIN-8, 12, 13 ou 14 com digito verificador certo. */
    static boolean eanValido(String ean) {
        if (!ean.matches("\\d{8}|\\d{12}|\\d{13}|\\d{14}")) {
            return false;
        }
        int soma = 0;
        int peso = 3;
        for (int i = ean.length() - 2; i >= 0; i--) {
            soma += (ean.charAt(i) - '0') * peso;
            peso = peso == 3 ? 1 : 3;
        }
        int verificador = (10 - soma % 10) % 10;
        return verificador == ean.charAt(ean.length() - 1) - '0';
    }

    private static RecursoNaoEncontradoException naoEncontrada() {
        return new RecursoNaoEncontradoException("Anúncio não encontrado.");
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}