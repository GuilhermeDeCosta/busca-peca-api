package br.com.buscapeca.buscapecaapi.loja.service;

import br.com.buscapeca.buscapecaapi.administracao.service.AuditoriaService;
import br.com.buscapeca.buscapecaapi.administracao.service.TermoUsoService;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.ConflitoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.loja.dto.CriarLojaRequest;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaPublicaResponse;
import br.com.buscapeca.buscapecaapi.loja.dto.LojaResponse;
import br.com.buscapeca.buscapecaapi.loja.entity.Loja;
import br.com.buscapeca.buscapecaapi.loja.enums.MetodoPagamento;
import br.com.buscapeca.buscapecaapi.loja.mapper.LojaMapper;
import br.com.buscapeca.buscapecaapi.loja.repository.LojaRepository;
import br.com.buscapeca.buscapecaapi.usuario.dto.EnderecoResponse;
import br.com.buscapeca.buscapecaapi.usuario.dto.UsuarioResponse;
import br.com.buscapeca.buscapecaapi.usuario.service.EnderecoService;
import br.com.buscapeca.buscapecaapi.usuario.service.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Map;

/**
 * Minha Loja (BP1-55/67). Dados do dono e endereco ficam no modulo usuario:
 * este servico chama UsuarioService e EnderecoService, nunca os repositorios deles (ADR-003).
 */
@Service
public class LojaService {

    private final LojaRepository lojaRepository;
    private final UsuarioService usuarioService;
    private final EnderecoService enderecoService;
    private final TermoUsoService termoUsoService;
    private final AuditoriaService auditoriaService;
    private final LojaMapper mapper;

    public LojaService(LojaRepository lojaRepository, UsuarioService usuarioService,
                       EnderecoService enderecoService, TermoUsoService termoUsoService,
                       AuditoriaService auditoriaService, LojaMapper mapper) {
        this.lojaRepository = lojaRepository;
        this.usuarioService = usuarioService;
        this.enderecoService = enderecoService;
        this.termoUsoService = termoUsoService;
        this.auditoriaService = auditoriaService;
        this.mapper = mapper;
    }

    /**
     * Cria a loja numa transacao so: grava o CPF/CNPJ no usuario, cria o endereco,
     * cria a loja com as formas de pagamento e da o papel VENDEDOR.
     * Qualquer erro desfaz tudo.
     */
    @Transactional
    public LojaResponse criar(Long idUsuario, CriarLojaRequest req) {
        if (lojaRepository.existsByIdUsuario(idUsuario)) {
            throw new ConflitoException("Você já tem uma loja. Para mudar os dados, use Configurações da loja.");
        }
        boolean retirada = Boolean.TRUE.equals(req.retiradaBalcao());
        boolean combinar = Boolean.TRUE.equals(req.entregaCombinar());
        boolean motoboy = Boolean.TRUE.equals(req.motoboyProprio());
        if (!retirada && !combinar && !motoboy) {
            throw new DadosInvalidosException("atendimento", "Marque pelo menos uma forma de atendimento.");
        }

        UsuarioResponse dono = usuarioService.habilitarVendedor(idUsuario, req.tipoPessoa(), req.cnpjCpf());
        EnderecoResponse endereco = enderecoService.cadastrar(idUsuario, req.endereco());
        termoUsoService.registrarAceiteDosVigentes(idUsuario);

        Loja loja = new Loja();
        loja.setIdUsuario(idUsuario);
        loja.setIdEndereco(endereco.idEndereco());
        loja.setNomeLoja(req.nomeLoja().trim());
        loja.setDescricao(vazioParaNulo(req.descricao()));
        loja.setTelefone(req.telefone());
        loja.setRetiradaBalcao(retirada);
        loja.setEntregaCombinar(combinar);
        loja.setMotoboyProprio(motoboy);
        loja.setMetodosPagamento(EnumSet.copyOf(req.metodosPagamento()));
        loja.setAceitaFaturamentoCnpj(req.metodosPagamento().contains(MetodoPagamento.FATURADO));
        loja = lojaRepository.save(loja);

        auditoriaService.registrar(idUsuario, "CRIAR_LOJA", "loja", Map.of("idLoja", loja.getId()));
        return mapper.paraResponse(loja, endereco, dono);
    }

    /** A loja do usuario logado (painel e configuracoes). 404 se ainda nao criou. */
    @Transactional(readOnly = true)
    public LojaResponse minha(Long idUsuario) {
        Loja loja = lojaRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Você ainda não tem uma loja."));
        EnderecoResponse endereco = enderecoService.buscar(idUsuario, loja.getIdEndereco());
        return mapper.paraResponse(loja, endereco, usuarioService.buscarPerfil(idUsuario));
    }

    /** Pagina publica da loja. Loja pausada ou inexistente: 404. */
    @Transactional(readOnly = true)
    public LojaPublicaResponse publica(Long idLoja) {
        Loja loja = lojaRepository.findByIdAndAtivaTrue(idLoja)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja não encontrada."));
        return mapper.paraPublica(loja, enderecoService.buscarPorId(loja.getIdEndereco()));
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
