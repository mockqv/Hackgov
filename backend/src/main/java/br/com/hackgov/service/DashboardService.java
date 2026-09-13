package br.com.hackgov.service;

import br.com.hackgov.dto.DashboardResponse;
import br.com.hackgov.repository.AvaliacaoRepository;
import br.com.hackgov.repository.SolicitacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final SolicitacaoRepository solicitacaoRepo;
    private final AvaliacaoRepository   avaliacaoRepo;

    public DashboardResponse gerarDashboard() {
        long total = 0, abertas = 0, emAndamento = 0, concluidas = 0, canceladas = 0;

        for (Object[] row : solicitacaoRepo.countByStatus()) {
            String status = row[0].toString();
            long count    = ((Number) row[1]).longValue();
            total += count;
            switch (status) {
                case "RECEBIDO"                        -> abertas     += count;
                case "EM_ANALISE", "AGENDADO",
                     "EM_EXECUCAO"                     -> emAndamento += count;
                case "CONCLUIDO"                       -> concluidas  += count;
                case "CANCELADO"                       -> canceladas  += count;
            }
        }

        double taxa = total > 0 ? Math.round((double) concluidas / total * 1000.0) / 10.0 : 0;

        List<DashboardResponse.CountPorTipo> countTipo = solicitacaoRepo.countByTipo().stream()
                .map(r -> DashboardResponse.CountPorTipo.builder()
                        .tipo(r[0].toString())
                        .quantidade(((Number) r[1]).longValue())
                        .build())
                .toList();

        List<DashboardResponse.TempoMedioTipo> tempoMedio = solicitacaoRepo.avgTempoResolucaoByTipo().stream()
                .map(r -> DashboardResponse.TempoMedioTipo.builder()
                        .tipo(r[0].toString())
                        .mediaDias(r[1] != null ? Math.round(((Number) r[1]).doubleValue() * 10.0) / 10.0 : null)
                        .build())
                .toList();

        Double notaMedia = avaliacaoRepo.findAvgNota();

        // % de solicitações concluídas dentro do SLA prometido.
        Double slaPercentual = null;
        List<Object[]> slaRows = solicitacaoRepo.slaCumprimento();
        if (!slaRows.isEmpty() && slaRows.get(0)[1] != null) {
            long dentroDoPrazo = slaRows.get(0)[0] != null ? ((Number) slaRows.get(0)[0]).longValue() : 0;
            long totalConcluidoComPrevisao = ((Number) slaRows.get(0)[1]).longValue();
            if (totalConcluidoComPrevisao > 0) {
                slaPercentual = Math.round((double) dentroDoPrazo / totalConcluidoComPrevisao * 1000.0) / 10.0;
            }
        }

        List<DashboardResponse.RankingBairro> topBairros = solicitacaoRepo.topBairros().stream()
                .map(r -> DashboardResponse.RankingBairro.builder()
                        .bairro(r[0].toString())
                        .quantidade(((Number) r[1]).longValue())
                        .build())
                .toList();

        return DashboardResponse.builder()
                .totalSolicitacoes(total)
                .abertas(abertas)
                .emAndamento(emAndamento)
                .concluidas(concluidas)
                .canceladas(canceladas)
                .taxaConclusao(taxa)
                .notaMediaAvaliacao(notaMedia != null ? Math.round(notaMedia * 10.0) / 10.0 : null)
                .slaCumpridoPercentual(slaPercentual)
                .countPorTipo(countTipo)
                .tempoMedioPorTipo(tempoMedio)
                .topBairros(topBairros)
                .build();
    }
}
