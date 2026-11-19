package br.com.buscapeca.buscapecaapi.compartilhado.erro;

import br.com.buscapeca.buscapecaapi.compartilhado.erro.ErroResposta.CampoInvalido;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.NegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Tratador global de erros: toda excecao vira o mesmo formato de resposta.
 * Detalhes tecnicos ficam so no log (Documentacao do Back-end, secao 6).
 */
@RestControllerAdvice
public class TratadorGlobalErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalErros.class);

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErroResposta> negocio(NegocioException ex, HttpServletRequest req) {
        List<CampoInvalido> campos = ex instanceof DadosInvalidosException d
                ? d.getCampos().entrySet().stream().map(c -> new CampoInvalido(c.getKey(), c.getValue())).toList()
                : List.of();
        return responder(ex.getStatus(), ex.getCodigo(), ex.getMessage(), req, campos);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                // Valor com tipo errado (ex.: precoMin=abc): a mensagem do Spring vem em ingles
                .map(e -> new CampoInvalido(e.getField(),
                        e.isBindingFailure() ? "Valor em formato inválido." : e.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS",
                "Verifique os campos destacados.", req, campos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroResposta> validacaoParametros(HandlerMethodValidationException ex,
                                                            HttpServletRequest req) {
        List<CampoInvalido> campos = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new CampoInvalido(r.getMethodParameter().getParameterName(),
                                e.getDefaultMessage())))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS",
                "Verifique os campos destacados.", req, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResposta> restricao(ConstraintViolationException ex, HttpServletRequest req) {
        List<CampoInvalido> campos = ex.getConstraintViolations().stream()
                .map(v -> new CampoInvalido(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS",
                "Verifique os campos destacados.", req, campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErroResposta> requisicaoInvalida(Exception ex, HttpServletRequest req) {
        String mensagem = ex instanceof MissingServletRequestParameterException m
                ? "Informe o parâmetro '" + m.getParameterName() + "'."
                : "A requisição tem um formato inválido.";
        return responder(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", mensagem, req, List.of());
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErroResposta> acessoNegado(Exception ex, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, "ACESSO_NEGADO",
                "Você não tem permissão para esta ação.", req, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResposta> naoEncontrado(NoResourceFoundException ex, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Rota não encontrada.", req, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResposta> metodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NAO_PERMITIDO",
                "Método " + ex.getMethod() + " não é aceito nesta rota.", req, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResposta> tipoMidia(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "TIPO_NAO_SUPORTADO",
                "Envie os dados em JSON (Content-Type: application/json).", req, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResposta> arquivoGrande(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return responder(HttpStatus.CONTENT_TOO_LARGE, "ARQUIVO_MUITO_GRANDE",
                "O arquivo passa do tamanho máximo de 5 MB.", req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> integridade(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violacao de integridade em {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "CONFLITO",
                "Os dados conflitam com um registro existente.", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Ocorreu um erro inesperado. Tente novamente em instantes.", req, List.of());
    }

    private ResponseEntity<ErroResposta> responder(HttpStatus status, String codigo, String mensagem,
                                                   HttpServletRequest req, List<CampoInvalido> campos) {
        return ResponseEntity.status(status)
                .body(ErroResposta.de(status.value(), codigo, mensagem, req.getRequestURI(), campos));
    }
}
