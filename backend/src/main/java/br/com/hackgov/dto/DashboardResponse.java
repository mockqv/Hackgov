package br.com.hackgov.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private long totalSolicitacoes;
    private long abertas;
    private long emAndamento;
    private long concluidas;
    private long canceladas;
    private double taxaConclusao;
    private Double notaMediaAvaliacao;
    /** % de solicitações concluídas dentro do prazo (dataConclusao <= dataPrevisao). null = sem base de cálculo. */
    private Double slaCumpridoPercentual;
    private List<CountPorTipo> countPorTipo;
    private List<TempoMedioTipo> tempoMedioPorTipo;
    private List<RankingBairro> topBairros;

    @Data
    @Builder
    public static class CountPorTipo {
        private String tipo;
        private Long quantidade;
    }

    @Data
    @Builder
    public static class TempoMedioTipo {
        private String tipo;
        private Double mediaDias;
    }

    @Data
    @Builder
    public static class RankingBairro {
        private String bairro;
        private Long quantidade;
    }
}
