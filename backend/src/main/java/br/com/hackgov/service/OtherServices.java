package br.com.hackgov.service;

import br.com.hackgov.dto.*;
import br.com.hackgov.exception.BusinessException;
import br.com.hackgov.exception.NotFoundException;
import br.com.hackgov.model.Cidadao;
import br.com.hackgov.repository.CidadaoRepository;
import br.com.hackgov.repository.SolicitacaoRepository;
import br.com.hackgov.repository.AvaliacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

// ── CIDADÃO ──────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Transactional
class CidadaoService {

    private final CidadaoRepository cidadaoRepo;

    public CidadaoResponse cadastrar(CidadaoRequest req) {
        if (cidadaoRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new BusinessException("E-mail já cadastrado");
        }

        Cidadao c = Cidadao.builder()
                .nome(req.getNome())
                .email(req.getEmail())
                .cpfHash(sha256(req.getCpf()))   // LGPD: armazena só o hash
                .telefone(req.getTelefone())
                .build();

        return CidadaoResponse.from(cidadaoRepo.save(c));
    }

    @Transactional(readOnly = true)
    public CidadaoResponse buscarPorId(Long id) {
        return CidadaoResponse.from(
            cidadaoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Cidadão não encontrado"))
        );
    }

    // SHA-256 do CPF para conformidade LGPD
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash", e);
        }
    }
}

// ── DASHBOARD ────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class DashboardService {

    private final SolicitacaoRepository solicitacaoRepo;
    private final AvaliacaoRepository   avaliacaoRepo;

    public DashboardResponse gerarDashboard() {

        // Contagem por status
        long total = 0, abertas = 0, emAndamento = 0, concluidas = 0, canceladas = 0;

        for (Object[] row : solicitacaoRepo.countByStatus()) {
            String status = row[0].toString();
            long count    = ((Number) row[1]).longValue();
            total += count;
            switch (status) {
                case "RECEBIDO"    -> abertas     += count;
                case "EM_ANALISE",
                     "AGENDADO",
                     "EM_EXECUCAO" -> emAndamento += count;
                case "CONCLUIDO"  -> concluidas   += count;
                case "CANCELADO"  -> canceladas   += count;
            }
        }

        double taxa = total > 0 ? (double) concluidas / total * 100 : 0;

        // Contagem por tipo
        List<DashboardResponse.CountPorTipo> countTipo = solicitacaoRepo.countByTipo().stream()
                .map(r -> DashboardResponse.CountPorTipo.builder()
                        .tipo(r[0].toString())
                        .quantidade(((Number) r[1]).longValue())
                        .build())
                .toList();

        // Tempo médio de resolução por tipo
        List<DashboardResponse.TempoMedioTipo> tempoMedio = solicitacaoRepo.avgTempoResolucaoByTipo().stream()
                .map(r -> DashboardResponse.TempoMedioTipo.builder()
                        .tipo(r[0].toString())
                        .mediaDias(r[1] != null ? ((Number) r[1]).doubleValue() : null)
                        .build())
                .toList();

        // Nota média de avaliações
        Double notaMedia = avaliacaoRepo.findAvgNota();

        return DashboardResponse.builder()
                .totalSolicitacoes(total)
                .abertas(abertas)
                .emAndamento(emAndamento)
                .concluidas(concluidas)
                .canceladas(canceladas)
                .taxaConclusao(Math.round(taxa * 10.0) / 10.0)
                .notaMediaAvaliacao(notaMedia != null ? Math.round(notaMedia * 10.0) / 10.0 : null)
                .countPorTipo(countTipo)
                .tempoMedioPorTipo(tempoMedio)
                .build();
    }
}
