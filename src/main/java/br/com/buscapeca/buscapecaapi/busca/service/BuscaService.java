package br.com.buscapeca.buscapecaapi.busca.service;

import br.com.buscapeca.buscapecaapi.busca.dto.BuscaFiltros;
import br.com.buscapeca.buscapecaapi.busca.dto.BuscaResposta;
import br.com.buscapeca.buscapecaapi.busca.dto.ResultadoBusca;
import br.com.buscapeca.buscapecaapi.busca.enums.OrdenacaoBusca;
import br.com.buscapeca.buscapecaapi.busca.repository.BuscaRepository;
import br.com.buscapeca.buscapecaapi.busca.repository.CriteriosBusca;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.paginacao.Paginacao;
import br.com.buscapeca.buscapecaapi.loja.enums.MetodoPagamento;
import br.com.buscapeca.buscapecaapi.peca.enums.CondicaoPeca;
import br.com.buscapeca.buscapecaapi.peca.enums.OrigemPeca;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Busca publica de pecas (BP1-158 texto e codigo, BP1-150 filtros, BP1-152 ordenacao).
 * Confere e normaliza os parametros e entrega ao BuscaRepository.
 *
 * Pagina comeca em 1, como no front (combinado em 17/11/2026); o restante da API
 * continua com o Paginacao, que comeca em 0.
 */
@Service
public class BuscaService {

    /** Menor palavra que o indice FULLTEXT do InnoDB guarda (innodb_ft_min_token_size). */
    private static final int MENOR_PALAVRA_INDICE = 3;
    private static final int PAGINA_MAXIMA = 10_000;

    /**
     * Palavras de 3 letras ou mais da lista padrao de stopwords do InnoDB: o indice nao as
     * guarda, e com "+" na frente poderiam impedir o resultado (ex.: "filtro com tampa").
     */
    private static final Set<String> STOPWORDS_INNODB = Set.of(
            "about", "are", "com", "for", "from", "how", "that", "the", "this", "was",
            "what", "when", "where", "who", "will", "with", "und", "www");

    /** Caracteres que tem significado no MATCH ... IN BOOLEAN MODE. */
    private static final Pattern OPERADORES_FULLTEXT = Pattern.compile("[+\\-<>()~*\"@]");
    /** Mesmo padrao do PecaService para gravar o OEM. */
    private static final Pattern SEPARADORES_CODIGO = Pattern.compile("[\\s.\\-]");

    private final BuscaRepository repository;

    public BuscaService(BuscaRepository repository) {
        this.repository = repository;
    }

    public BuscaResposta buscar(BuscaFiltros f) {
        Map<String, String> erros = new LinkedHashMap<>();

        CondicaoPeca condicao = enumOuErro(f.condicao(), CondicaoPeca.class, "condicao",
                "Condição inválida. Use NOVA, USADA ou RECONDICIONADA.", erros);
        OrigemPeca origem = enumOuErro(f.origem(), OrigemPeca.class, "origem",
                "Origem inválida. Use NACIONAL ou IMPORTADA.", erros);
        MetodoPagamento formaPagamento = enumOuErro(f.formaPagamento(), MetodoPagamento.class, "formaPagamento",
                "Forma de pagamento inválida.", erros);
        OrdenacaoBusca ordenacao = OrdenacaoBusca.de(f.ordenar());
        if (ordenacao == null) {
            erros.put("ordenar", "Ordenação inválida. Use relevancia, preco, preco_desc, distancia ou avaliacao.");
        }
        List<Long> veiculos = idsOuErro(f.veiculo(), Long::valueOf, "veiculo", erros);
        List<Integer> tags = idsOuErro(f.tags(), Integer::valueOf, "tags", erros);

        if (f.precoMin() != null && f.precoMin().signum() < 0) {
            erros.put("precoMin", "O preço mínimo não pode ser negativo.");
        }
        if (f.precoMax() != null && f.precoMax().signum() < 0) {
            erros.put("precoMax", "O preço máximo não pode ser negativo.");
        }
        if (f.precoMin() != null && f.precoMax() != null && f.precoMin().compareTo(f.precoMax()) > 0) {
            erros.put("precoMin", "O preço mínimo não pode ser maior que o máximo.");
        }
        if (f.notaMinima() != null
                && (f.notaMinima().signum() < 0 || f.notaMinima().compareTo(BigDecimal.valueOf(5)) > 0)) {
            erros.put("notaMinima", "A nota mínima vai de 0 a 5.");
        }
        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }

        String texto = vazioParaNulo(f.q());
        String codigo = vazioParaNulo(f.codigo());
        CriteriosBusca criterios = new CriteriosBusca(
                texto == null ? null : textoFulltext(texto),
                texto == null ? null : "%" + escaparLike(texto) + "%",
                texto == null ? null : codigoNormalizado(texto),
                texto == null ? null : texto.toUpperCase(Locale.ROOT),
                codigo == null ? null : codigoNormalizado(codigo),
                codigo == null ? null : codigo.toUpperCase(Locale.ROOT),
                f.categoria(),
                veiculos,
                tags,
                condicao,
                origem,
                f.prontaEntrega(),
                formaPagamento,
                f.notaMinima(),
                f.retirada(),
                f.freteGratis(),
                f.precoMin(),
                f.precoMax(),
                ordenacao);

        int pagina = f.pagina() == null || f.pagina() < 1 ? 1 : Math.min(f.pagina(), PAGINA_MAXIMA);
        int tamanho = f.tamanho() == null || f.tamanho() < 1
                ? Paginacao.TAMANHO_PADRAO
                : Math.min(f.tamanho(), Paginacao.TAMANHO_MAXIMO);

        BuscaRepository.Totais totais = repository.contar(criterios);
        List<ResultadoBusca> itens = totais.registros() == 0
                ? List.of()
                : repository.buscar(criterios, tamanho, (long) (pagina - 1) * tamanho);
        int totalPaginas = (int) ((totais.registros() + tamanho - 1) / tamanho);

        return new BuscaResposta(itens, pagina, tamanho, totais.registros(), totalPaginas, totais.lojas());
    }

    /**
     * "pastilha de freio" vira "+pastilha* +freio*": todas as palavras precisam aparecer,
     * e "pastil" tambem acha "pastilha". Palavras com menos de 3 letras ficam de fora,
     * porque o indice nao as guarda, e as stopwords do InnoDB tambem. Sem nenhuma palavra util, devolve nulo (o
     * repositorio usa LIKE no titulo).
     */
    static String textoFulltext(String texto) {
        String termos = Arrays.stream(OPERADORES_FULLTEXT.matcher(texto).replaceAll(" ").split("\\s+"))
                .filter(p -> p.length() >= MENOR_PALAVRA_INDICE)
                .filter(p -> !STOPWORDS_INNODB.contains(p.toLowerCase(Locale.ROOT)))
                .map(p -> "+" + p + "*")
                .collect(Collectors.joining(" "));
        return termos.isEmpty() ? null : termos;
    }

    static String codigoNormalizado(String valor) {
        String codigo = SEPARADORES_CODIGO.matcher(valor.toUpperCase(Locale.ROOT)).replaceAll("");
        return codigo.isEmpty() ? null : codigo;
    }

    /** % e _ sao curingas no LIKE; com barra antes, valem como texto. */
    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static <E extends Enum<E>> E enumOuErro(String valor, Class<E> tipo, String campo,
                                                    String mensagem, Map<String, String> erros) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            erros.put(campo, mensagem);
            return null;
        }
    }

    /** "12, 13,14" vira [12, 13, 14]. Texto vazio vira lista vazia. */
    private static <T> List<T> idsOuErro(String valor, Function<String, T> conversor, String campo,
                                         Map<String, String> erros) {
        List<T> ids = new ArrayList<>();
        if (valor == null || valor.isBlank()) {
            return ids;
        }
        try {
            for (String parte : valor.split(",")) {
                if (!parte.isBlank()) {
                    ids.add(conversor.apply(parte.trim()));
                }
            }
        } catch (NumberFormatException e) {
            erros.put(campo, "Use ids numéricos separados por vírgula.");
            return List.of();
        }
        return ids;
    }
}
