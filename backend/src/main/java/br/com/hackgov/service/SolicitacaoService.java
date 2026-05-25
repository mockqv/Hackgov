package br.com.hackgov.service;

import br.com.hackgov.dto.*;
import br.com.hackgov.exception.BusinessException;
import br.com.hackgov.exception.NotFoundException;
import br.com.hackgov.model.*;
import br.com.hackgov.repository.*;
import br.com.hackgov.security.AuthUtils;
import br.com.hackgov.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SolicitacaoService {

    private final SolicitacaoRepository    solicitacaoRepo;
    private final CidadaoRepository        cidadaoRepo;
    private final TipoServicoRepository    tipoServicoRepo;
    private final LocalizacaoRepository    localizacaoRepo;
    private final BairroRepository         bairroRepo;
    private final ServidorRepository       servidorRepo;
    private final HistoricoStatusRepository historicoRepo;
    private final AvaliacaoRepository      avaliacaoRepo;

    // ── CRIAR SOLICITAÇÃO (usuário autenticado = CIDADAO) ────
    public SolicitacaoDetalheResponse criar(SolicitacaoRequest req) {

        AuthenticatedUser principal = AuthUtils.require();
        if (principal.getKind() != AuthenticatedUser.Kind.CIDADAO) {
            throw new BusinessException("Apenas cidadãos podem abrir solicitações");
        }

        Cidadao cidadao = cidadaoRepo.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Cidadão não encontrado"));

        TipoServico tipo = tipoServicoRepo.findById(req.getTipoServicoId())
                .orElseThrow(() -> new NotFoundException("Tipo de serviço não encontrado"));

        Bairro bairro = bairroRepo.findById(req.getBairroId())
                .orElseThrow(() -> new NotFoundException("Bairro não encontrado"));

        List<Solicitacao> duplicatas = solicitacaoRepo
                .findDuplicatas(req.getLatitude(), req.getLongitude(), tipo.getId());

        if (!duplicatas.isEmpty()) {
            Solicitacao dup = duplicatas.get(0);
            log.info("Duplicata detectada: {}", dup.getProtocolo());
            throw new BusinessException(
                "Já existe uma solicitação aberta próxima a este local. " +
                "Protocolo existente: " + dup.getProtocolo()
            );
        }

        Localizacao loc = Localizacao.builder()
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .logradouro(req.getLogradouro())
                .numero(req.getNumero())
                .complemento(req.getComplemento())
                .cep(req.getCep())
                .bairro(bairro)
                .build();

        LocalDate previsao = LocalDate.now().plusDays(tipo.getSlaDias());

        Solicitacao solic = Solicitacao.builder()
                .cidadao(cidadao)
                .tipoServico(tipo)
                .localizacao(loc)
                .descricao(req.getDescricao())
                .caminhoFotoAntes(req.getCaminhoFotoAntes())
                .dataPrevisao(previsao)
                .build();

        Solicitacao salva = solicitacaoRepo.save(solic);
        log.info("Solicitação criada: {} por cidadão {}", salva.getProtocolo(), cidadao.getId());

        return SolicitacaoDetalheResponse.from(salva);
    }

    // ── BUSCAR POR PROTOCOLO (público) ───────────────────────
    @Transactional(readOnly = true)
    public SolicitacaoDetalheResponse buscarPorProtocolo(String protocolo) {
        Solicitacao s = solicitacaoRepo.findByProtocolo(protocolo.toUpperCase())
                .orElseThrow(() -> new NotFoundException("Protocolo não encontrado: " + protocolo));
        return SolicitacaoDetalheResponse.from(s);
    }

    // ── BUSCAR POR ID (interno) ──────────────────────────────
    @Transactional(readOnly = true)
    public SolicitacaoDetalheResponse buscarPorId(Long id) {
        Solicitacao s = solicitacaoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));
        return SolicitacaoDetalheResponse.from(s);
    }

    // ── LISTAR DO CIDADÃO LOGADO ─────────────────────────────
    @Transactional(readOnly = true)
    public List<SolicitacaoResumoResponse> listarMinhas() {
        AuthenticatedUser principal = AuthUtils.require();
        if (principal.getKind() != AuthenticatedUser.Kind.CIDADAO) {
            throw new BusinessException("Apenas cidadãos têm 'minhas solicitações'");
        }
        return solicitacaoRepo.findByCidadaoIdOrderByDataAberturaDesc(principal.getId())
                .stream().map(SolicitacaoResumoResponse::from).toList();
    }

    // ── LISTAR TODAS ABERTAS (servidor/gestor) ───────────────
    @Transactional(readOnly = true)
    public List<SolicitacaoResumoResponse> listarAbertas() {
        return solicitacaoRepo.findTodasAbertas()
                .stream().map(SolicitacaoResumoResponse::from).toList();
    }

    // ── LISTAR TODAS (mapa público) ───────────────────────────
    @Transactional(readOnly = true)
    public List<SolicitacaoResumoResponse> listarParaMapa() {
        return solicitacaoRepo.findAllParaMapa()
                .stream().map(SolicitacaoResumoResponse::from).toList();
    }

    // ── ATUALIZAR STATUS (servidor autenticado) ──────────────
    public SolicitacaoDetalheResponse atualizarStatus(Long id, AtualizarStatusRequest req) {

        AuthenticatedUser principal = AuthUtils.require();
        if (principal.getKind() != AuthenticatedUser.Kind.SERVIDOR) {
            throw new BusinessException("Apenas servidores podem atualizar status");
        }

        Solicitacao solic = solicitacaoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        Servidor servidor = servidorRepo.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Servidor não encontrado"));

        StatusSolicitacao novoStatus = req.getNovoStatus();
        StatusSolicitacao atual = solic.getStatus();

        if (atual == StatusSolicitacao.CONCLUIDO || atual == StatusSolicitacao.CANCELADO) {
            throw new BusinessException("Solicitação já está encerrada com status: " + atual);
        }

        if (novoStatus == StatusSolicitacao.CONCLUIDO && req.getCaminhoFotoDepois() != null) {
            solic.setCaminhoFotoDepois(req.getCaminhoFotoDepois());
        }

        HistoricoStatus hist = HistoricoStatus.builder()
                .solicitacao(solic)
                .servidor(servidor)
                .statusAnterior(atual)
                .statusNovo(novoStatus)
                .justificativa(req.getJustificativa())
                .build();
        historicoRepo.save(hist);

        solic.setServidor(servidor);
        solic.atualizarStatus(novoStatus);

        log.info("Status atualizado: {} {} -> {} por servidor {}",
                solic.getProtocolo(), atual, novoStatus, servidor.getId());

        return SolicitacaoDetalheResponse.from(solicitacaoRepo.save(solic));
    }

    // ── BUSCAR HISTÓRICO ──────────────────────────────────────
    @Transactional(readOnly = true)
    public List<HistoricoStatusResponse> buscarHistorico(Long id) {
        return historicoRepo.findBySolicitacaoIdOrderByDataHoraAsc(id)
                .stream().map(HistoricoStatusResponse::from).toList();
    }

    // ── AVALIAR SERVIÇO (cidadão autor da solicitação) ───────
    public void avaliar(Long id, AvaliacaoRequest req) {

        AuthenticatedUser principal = AuthUtils.require();
        if (principal.getKind() != AuthenticatedUser.Kind.CIDADAO) {
            throw new BusinessException("Apenas cidadãos podem avaliar");
        }

        Solicitacao solic = solicitacaoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        if (!solic.getCidadao().getId().equals(principal.getId())) {
            throw new BusinessException("Você só pode avaliar suas próprias solicitações");
        }

        if (solic.getStatus() != StatusSolicitacao.CONCLUIDO) {
            throw new BusinessException("Avaliação só disponível para solicitações concluídas");
        }

        if (avaliacaoRepo.findBySolicitacaoId(id).isPresent()) {
            throw new BusinessException("Esta solicitação já foi avaliada");
        }

        Avaliacao aval = Avaliacao.builder()
                .solicitacao(solic)
                .nota(req.getNota())
                .comentario(req.getComentario())
                .build();

        avaliacaoRepo.save(aval);
        log.info("Avaliação registrada: solicitação {} nota {}", solic.getProtocolo(), req.getNota());
    }
}
