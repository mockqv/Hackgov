package br.com.hackgov.dto;

import java.time.LocalDate;
import java.util.List;

/** Relatório estatístico da Fase 7: correlação e regressão sobre os dados do sistema. */
public record EstatisticaResponse(LocalDate referencia, int chamados, List<Analise> analises) {

    public record Analise(
            String id,
            String titulo,
            String pergunta,
            String eixoX,
            String eixoY,
            int n,
            double r,
            double r2,
            double inclinacao,
            double intercepto,
            double valorP,
            boolean significativa,
            String forca,
            String leitura,
            String decisao,
            List<Ponto> pontos,
            List<Projecao> projecao) {}

    public record Ponto(double x, double y, String rotulo) {}

    public record Projecao(String semana, double previsto, double minimo, double maximo) {}
}
