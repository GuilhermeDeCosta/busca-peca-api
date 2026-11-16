package br.com.buscapeca.buscapecaapi.usuario.service;

import br.com.buscapeca.buscapecaapi.compartilhado.excecao.AcessoNegadoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.ConflitoException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.DadosInvalidosException;
import br.com.buscapeca.buscapecaapi.compartilhado.excecao.RecursoNaoEncontradoException;
import br.com.buscapeca.buscapecaapi.integracao.geolocalizacao.GeolocalizacaoService;
import br.com.buscapeca.buscapecaapi.usuario.dto.CepResponse;
import br.com.buscapeca.buscapecaapi.usuario.dto.EnderecoRequest;
import br.com.buscapeca.buscapecaapi.usuario.dto.EnderecoResponse;
import br.com.buscapeca.buscapecaapi.usuario.entity.Endereco;
import br.com.buscapeca.buscapecaapi.usuario.mapper.EnderecoMapper;
import br.com.buscapeca.buscapecaapi.usuario.repository.EnderecoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Enderecos do usuario (BP1-60) e consulta de CEP (BP1-7).
 * Regras: so o dono mexe no endereco; o primeiro vira principal;
 * existe sempre um, e so um, endereco principal.
 * Outros modulos (ex.: loja) usam este servico, nunca o EnderecoRepository (ADR-003).
 */
@Service
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final EnderecoMapper mapper;
    private final GeolocalizacaoService geolocalizacao;

    public EnderecoService(EnderecoRepository enderecoRepository, EnderecoMapper mapper,
                           GeolocalizacaoService geolocalizacao) {
        this.enderecoRepository = enderecoRepository;
        this.mapper = mapper;
        this.geolocalizacao = geolocalizacao;
    }

    @Transactional(readOnly = true)
    public List<EnderecoResponse> listar(Long idUsuario) {
        return enderecoRepository.findByIdUsuarioOrderByPrincipalDescIdAsc(idUsuario).stream()
                .map(mapper::paraResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnderecoResponse buscar(Long idUsuario, Long idEndereco) {
        return mapper.paraResponse(buscarDoUsuario(idUsuario, idEndereco));
    }

    /**
     * Endereco pelo id, sem conferir o dono. So para outros modulos que ja
     * conferiram o acesso (ex.: pagina publica da loja mostra cidade e UF).
     */
    @Transactional(readOnly = true)
    public EnderecoResponse buscarPorId(Long idEndereco) {
        return enderecoRepository.findById(idEndereco)
                .map(mapper::paraResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado."));
    }

    /** O primeiro endereco do usuario vira principal sozinho. */
    @Transactional
    public EnderecoResponse cadastrar(Long idUsuario, EnderecoRequest req) {
        boolean primeiro = enderecoRepository.countByIdUsuario(idUsuario) == 0;
        boolean principal = primeiro || Boolean.TRUE.equals(req.principal());
        if (principal && !primeiro) {
            enderecoRepository.desmarcarPrincipal(idUsuario);
        }

        Endereco endereco = new Endereco();
        endereco.setIdUsuario(idUsuario);
        preencher(endereco, req);
        endereco.setPrincipal(principal);
        return mapper.paraResponse(enderecoRepository.save(endereco));
    }

    /**
     * Altera os dados. principal = true troca o principal; principal = false
     * no endereco principal e ignorado, porque sempre precisa existir um.
     */
    @Transactional
    public EnderecoResponse atualizar(Long idUsuario, Long idEndereco, EnderecoRequest req) {
        Endereco endereco = buscarDoUsuario(idUsuario, idEndereco);
        if (Boolean.TRUE.equals(req.principal()) && !endereco.isPrincipal()) {
            // desmarcarPrincipal limpa a sessao do JPA; o save abaixo grava tudo de novo
            enderecoRepository.desmarcarPrincipal(idUsuario);
            endereco.setPrincipal(true);
        }
        preencher(endereco, req);
        return mapper.paraResponse(enderecoRepository.save(endereco));
    }

    @Transactional
    public EnderecoResponse definirPrincipal(Long idUsuario, Long idEndereco) {
        Endereco endereco = buscarDoUsuario(idUsuario, idEndereco);
        if (!endereco.isPrincipal()) {
            enderecoRepository.desmarcarPrincipal(idUsuario);
            endereco.setPrincipal(true);
            endereco = enderecoRepository.save(endereco);
        }
        return mapper.paraResponse(endereco);
    }

    /**
     * Remove o endereco. Se era o principal, o mais antigo que sobrou vira principal.
     * O endereco usado pela loja nao pode ser removido (loja.id_endereco).
     */
    @Transactional
    public void remover(Long idUsuario, Long idEndereco) {
        Endereco endereco = buscarDoUsuario(idUsuario, idEndereco);
        boolean eraPrincipal = endereco.isPrincipal();
        try {
            enderecoRepository.delete(endereco);
            enderecoRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoException(
                    "Este endereço é usado pela sua loja. Troque o endereço da loja antes de removê-lo.");
        }
        if (eraPrincipal) {
            enderecoRepository.findFirstByIdUsuarioOrderByIdAsc(idUsuario)
                    .ifPresent(outro -> outro.setPrincipal(true));
        }
    }

    /** Rua, bairro, cidade e UF do CEP, para preencher o formulario no front. */
    public CepResponse consultarCep(String cep) {
        String digitos = cep == null ? "" : cep.replaceAll("\\D", "");
        if (digitos.length() != 8) {
            throw new DadosInvalidosException("cep", "Informe um CEP com 8 números.");
        }
        return geolocalizacao.buscarEndereco(digitos)
                .map(e -> new CepResponse(digitos, e.logradouro(), e.bairro(), e.cidade(), e.estado()))
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "CEP não encontrado. Preencha o endereço manualmente."));
    }

    // ---- uso interno ----

    /** 404 se nao existe; 403 se existe, mas e de outro usuario. */
    private Endereco buscarDoUsuario(Long idUsuario, Long idEndereco) {
        Endereco endereco = enderecoRepository.findById(idEndereco)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado."));
        if (!endereco.getIdUsuario().equals(idUsuario)) {
            throw new AcessoNegadoException("Este endereço não pertence à sua conta.");
        }
        return endereco;
    }

    /** Copia os campos do request; busca as coordenadas quando o CEP e novo ou mudou. */
    private void preencher(Endereco endereco, EnderecoRequest req) {
        boolean cepMudou = !req.cep().equals(endereco.getCep());

        endereco.setIdentificacao(vazioParaNulo(req.identificacao()));
        endereco.setCep(req.cep());
        endereco.setLogradouro(req.logradouro().trim());
        endereco.setNumero(req.numero().trim());
        endereco.setComplemento(vazioParaNulo(req.complemento()));
        endereco.setBairro(req.bairro().trim());
        endereco.setCidade(req.cidade().trim());
        endereco.setEstado(req.estado().trim().toUpperCase(Locale.ROOT));

        if (cepMudou) {
            // Sem coordenadas o endereco e salvo mesmo assim (ADR-012)
            var coordenadas = geolocalizacao.buscarCoordenadas(req.cep()).orElse(null);
            endereco.setLatitude(coordenadas == null ? null : coordenadas.latitude());
            endereco.setLongitude(coordenadas == null ? null : coordenadas.longitude());
        }
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
