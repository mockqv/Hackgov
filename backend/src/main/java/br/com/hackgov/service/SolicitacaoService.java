package br.com.hackgov.service;

import br.com.hackgov.dto.*;
import br.com.hackgov.exception.BusinessException;
import br.com.hackgov.exception.NotFoundException;
import br.com.hackgov.model.*;
import br.com.hackgov.repository.*;
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

    // ── CRIAR SOLICITAÇÃO ────────────────────────────────────
    public SolicitacaoDetalheResponse criar(SolicitacaoRequest req) {

        // 1. Buscar entidades
        Cidadao cidadao = cidadaoRepo.findById(req.getCidadaoId())
                .orElseThrow(() -> new NotFoundException("Cidadão não encontrado"));

        TipoServico tipo = tipoServicoRepo.findById(req.getTipoServicoId())
                .orElseThrow(() -> new NotFoundException("Tipo de serviço não encontrado"));

        Bairro bairro = bairroRepo.findById(req.getBairroId())
                .orElseThrow(() -> new NotFoundException("Bairro não encontrado"));

        // 2. Verificar duplicata (raio 10m, mesmo tipo, status aberto)
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

        // 3. Criar localização
        Localizacao loc = Localizacao.builder()
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .logradouro(req.getLogradouro())
                .numero(req.getNumero())
                .complemento(req.getComplemento())
                .cep(req.getCep())
                .bairro(bairro)
                .build();

        // 4. Calcular previsão baseada no SLA
        LocalDate previsao = LocalDate.now().plusDays(tipo.getSlaDias());

        // 5. Criar e salvar solicitação
        Solicitacao solic = Solicitacao.builder()
                .cidadao(cidadao)
                .tipoServico(tipo)
                .localizacao(loc)
                .descricao(req.getDescricao())
                .caminhoFotoAntes(req.getCaminhoFotoAntes())
                .dataPrevisao(previsao)
                .build();

        Solicitacao salva = solicitacaoRepo.save(solic);
        log.info("Solicitação criada: {}", salva.getProtocolo());

        return SolicitacaoDetalheResponse.from(salva);
    }

    // ── BUSCAR POR PROTOCOLO ─────────────────────────────────
    @Transactional(readOnly = true)
    public SolicitacaoDetalheResponse buscarPorProtocolo(String protocolo) {
        Solicitacao s = solicitacaoRepo.findByProtocolo(protocolo.toUpperCase())
                .orElseThrow(() -> new NotFoundException("Protocolo não encontrado: " + protocolo));
        return SolicitacaoDetalheResponse.from(s);
    }

    // ── BUSCAR POR ID ────────────────────────────────────────
    @Transactional(readOnly = true)
    public SolicitacaoDetalheResponse buscarPorId(Long id) {
        Solicitacao s = solicitacaoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));
        return SolicitacaoDetalheResponse.from(s);
    }

    // ── LISTAR DO CIDADÃO ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<SolicitacaoResumoResponse> listarPorCidadao(Long cidadaoId) {
        return solicitacaoRepo.findByCidadaoIdOrderByDataAberturaDesc(cidadaoId)
                .stream().map(SolicitacaoResumoResponse::from).toList();
    }

    // ── LISTAR TODAS ABERTAS (painel servidor) ────────────────
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

    // ── ATUALIZAR STATUS ──────────────────────────────────────
    public SolicitacaoDetalheResponse atualizarStatus(Long id, AtualizarStatusRequest req) {

        Solicitacao solic = solicitacaoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        Servidor servidor = servidorRepo.findById(req.getServidorId())
                .orElseThrow(() -> new NotFoundException("Servidor não encontrado"));

        StatusSolicitacao novoStatus;
        try {
            novoStatus = StatusSolicitacao.valueOf(req.getNovoStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status inválido: " + req.getNovoStatus());
        }

        // Validar transição: não pode regredir depois de CONCLUIDO/CANCELADO
        if (solic.getStatus() == StatusSolicitacao.CONCLUIDO ||
            solic.getStatus() == StatusSolicitacao.CANCELADO) {
            throw new BusinessException("Solicitação já está encerrada com status: " + solic.getStatus());
        }

        // Registrar foto do depois se for concluir
        if (novoStatus == StatusSolicitacao.CONCLUIDO && req.getCaminhoFotoDepois() != null) {
            solic.setCaminhoFotoDepois(req.getCaminhoFotoDepois());
        }

        // Registrar histórico (imutável)
        HistoricoStatus hist = HistoricoStatus.builder()
                .solicitacao(solic)
                .servidor(servidor)
                .statusAnterior(solic.getStatus())
                .statusNovo(novoStatus)
                .justificativa(req.getJustificativa())
                .build();
        historicoRepo.save(hist);

        // Atribuir servidor responsável e atualizar status
        solic.setServidor(servidor);
        solic.atualizarStatus(novoStatus);

        return SolicitacaoDetalheResponse.from(solicitacaoRepo.save(solic));
    }

    // ── BUSCAR HISTÓRICO ──────────────────────────────────────
    @Transactional(readOnly = true)
    public List<HistoricoStatusResponse> buscarHistorico(Long id) {
        return historicoRepo.findBySolicitacaoIdOrderByDataHoraAsc(id)
                .stream().map(HistoricoStatusResponse::from).toList();
    }

    // ── AVALIAR SERVIÇO ───────────────────────────────────────
    public void avaliar(Long id, AvaliacaoRequest req) {

        Solicitacao solic = solicitacaoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));

        if (solic.getStatus() != StatusSolicitacao.CONCLUIDO) {
            throw new BusinessException("Avaliação só disponível para solicitações CONCLUÍDAS");
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
        log.info("Avaliação registrada para solicitação {}: {} estrela(s)", solic.getProtocolo(), req.getNota());
    }
}
