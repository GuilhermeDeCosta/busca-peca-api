package br.com.buscapeca.buscapecaapi.peca.repository;

import br.com.buscapeca.buscapecaapi.peca.entity.Peca;
import br.com.buscapeca.buscapecaapi.peca.enums.NivelEstoque;
import br.com.buscapeca.buscapecaapi.peca.enums.StatusPeca;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros de GET /api/pecas?loja=minha (BP1-225), ja conferidos pelo PecaService.
 * Excluidos nunca entram. O MySQL compara sem diferenciar maiusculas (utf8mb4_0900_ai_ci),
 * por isso o LIKE vai direto na coluna.
 */
public final class FiltroMeusAnuncios {

    private static final char ESCAPE = '\\';

    private FiltroMeusAnuncios() {
    }

    /**
     * @param idLoja  loja de quem esta logado
     * @param status  ATIVO, PAUSADO ou RASCUNHO; nulo traz os tres
     * @param estoque faixa de estoque; nulo traz todas
     * @param termo   texto procurado no titulo e no SKU; nulo nao filtra
     * @param oem     o mesmo texto no formato do OEM (sem espacos, pontos e hifens); nulo nao procura no OEM
     */
    public static Specification<Peca> de(Long idLoja, StatusPeca status, NivelEstoque estoque,
                                         String termo, String oem) {
        return (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            condicoes.add(cb.equal(root.get("idLoja"), idLoja));
            condicoes.add(cb.notEqual(root.get("status"), StatusPeca.EXCLUIDO));
            if (status != null) {
                condicoes.add(cb.equal(root.get("status"), status));
            }
            if (estoque != null) {
                Path<Integer> quantidade = root.get("quantidadeEstoque");
                Path<Integer> limite = root.get("limiteAlertaEstoque");
                condicoes.add(switch (estoque) {
                    case ZERADO -> cb.le(quantidade, 0);
                    case BAIXO -> cb.and(cb.gt(quantidade, 0), cb.le(quantidade, limite));
                    case NORMAL -> cb.gt(quantidade, limite);
                });
            }
            if (termo != null) {
                List<Predicate> texto = new ArrayList<>();
                texto.add(cb.like(root.get("titulo"), contem(termo), ESCAPE));
                texto.add(cb.like(root.get("codigoSku"), contem(termo), ESCAPE));
                if (oem != null) {
                    texto.add(cb.like(root.get("codigoOem"), contem(oem), ESCAPE));
                }
                condicoes.add(cb.or(texto.toArray(Predicate[]::new)));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
    }

    /** %texto%, com % e _ digitados pelo vendedor tratados como letras comuns. */
    static String contem(String texto) {
        String escapado = texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
