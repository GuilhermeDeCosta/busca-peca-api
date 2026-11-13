package br.com.buscapeca.buscapecaapi.seguranca;

import br.com.buscapeca.buscapecaapi.config.AppProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Emite e valida o JWT da sessao (ADR-006): assinado com JWT_SEGREDO (HS256),
 * leva o id do usuario e os papeis, e vale JWT_EXPIRACAO_MINUTOS (padrao 120).
 */
@Service
public class JwtService {

    public static final String EMISSOR = "buscapeca-api";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration validade;

    public JwtService(AppProperties props) {
        String segredo = props.jwt() == null ? null : props.jwt().segredo();
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "Defina a variavel JWT_SEGREDO com pelo menos 32 caracteres (ADR-006).");
        }
        SecretKey chave = new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
        this.decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        long minutos = props.jwt().expiracaoMinutos() > 0 ? props.jwt().expiracaoMinutos() : 120;
        this.validade = Duration.ofMinutes(minutos);
    }

    public TokenEmitido emitir(Long idUsuario, List<String> papeis) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(validade);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(String.valueOf(idUsuario))
                .issuedAt(agora)
                .expiresAt(expira)
                .claim("papeis", papeis)
                .build();
        String valor = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenEmitido(valor, expira, validade);
    }

    public JwtDecoder decoder() {
        return decoder;
    }

    public record TokenEmitido(String valor, Instant expiraEm, Duration validade) {
    }
}
