package br.com.buscapeca.buscapecaapi.peca.service;

import br.com.buscapeca.buscapecaapi.catalogo.service.CatalogoService;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.AcessoNegadoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.loja.service.LojaService;
import br.com.buscapeca.buscapecaapi.peca.dto.AplicacaoRequest;
import br.com.buscapeca.buscapecaapi.peca.dto.AplicacaoResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.CompatibilidadesRequest;
import br.com.buscapeca.buscapecaapi.peca.dto.CompatibilidadesResponse;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaRequest;
import br.com.buscapeca.buscapecaapi.peca.dto.PecaResponse;
import br.com.buscapeca.buscapecaapi.peca.entity.Compatibilidade;
import br.com.buscapeca.buscapecaapi.peca.entity.Peca;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;
import br.com.buscapeca.buscapecaapi.peca.mapper.PecaMapper;
import br.com.buscapeca.buscapecaapi.peca.repository.CompatibilidadeRepository;
import br.com.buscapeca.buscapecaapi.peca.repository.PecaRepository;
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
 */
@Service
public class PecaService {

    private static final Pattern OEM = Pattern.compile("^[A-Z0-9/]{3,50}$");
    private static final Pattern SKU = Pattern.compile("^[A-Z0-9][A-Z0-9._\\-/]{1,49}$");
    private static final Pattern SEPARADORES_OEM = Pattern.compile("[\\s.\\-]");
    private static final int FAIXA_MAXIMA_ANOS = 40;
    private static final int VEICULOS_MAXIMOS = 500;

    private final PecaRepository pecaRepository;
    private final CompatibilidadeRepository compatibilidadeRepository;
    private final LojaService lojaService;
    private final CatalogoService catalogoService;
    private final PecaMapper mapper;

    public PecaService(PecaRepository pecaRepository, CompatibilidadeRepository compatibilidadeRepository,
                       LojaService lojaService, CatalogoService catalogoService, PecaMapper mapper) {
        this.pecaRepository = pecaRepository;
        this.compatibilidadeRepository = compatibilidadeRepository;
        this.lojaService = lojaService;
        this.catalogoService = catalogoService;
        this.mapper = mapper;
    }

    /** Cria o anuncio na loja de quem esta logado, como rascunho ou ja publicado. */
    @Transactional
    public PecaResponse criar(Long idUsuario, PecaRequest req) {
        Peca peca = new Peca();
        peca.setIdLoja(lojaService.idDaLojaParaAnunciar(idUsuario));
        preencher(peca, req);
        peca = pecaRepository.saveAndFlush(peca);
        return mapper.paraResponse(peca, List.of());
    }

    /** Edita o anuncio inteiro. So a loja dona da peca. */
    @Transactional
    public PecaResponse atualizar(Long idUsuario, Long idPeca, PecaRequest req) {
        Peca peca = daMinhaLoja(idUsuario, idPeca);
        preencher(peca, req);
        peca = pecaRepository.saveAndFlush(peca);
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
