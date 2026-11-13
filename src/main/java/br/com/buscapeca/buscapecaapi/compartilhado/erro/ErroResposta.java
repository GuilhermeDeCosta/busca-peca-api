package br.com.buscapeca.buscapecaapi.compartilhado.erro;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Formato unico de erro da API (Documentacao do Back-end, secao 6).
 * A mensagem e em portugues, pronta para o front exibir.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroResposta(
        OffsetDateTime dataHora,
        int status,
        String erro,
        String mensagem,
        String rota,
        List<CampoInvalido> campos
) {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    public static ErroResposta de(int status, String erro, String mensagem, String rota) {
        return new ErroResposta(OffsetDateTime.now(FUSO), status, erro, mensagem, rota, List.of());
    }

    public static ErroResposta de(int status, String erro, String mensagem, String rota,
                                  List<CampoInvalido> campos) {
        return new ErroResposta(OffsetDateTime.now(FUSO), status, erro, mensagem, rota, campos);
    }

    public record CampoInvalido(String campo, String mensagem) {
    }
}
