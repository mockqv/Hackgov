package br.com.hackgov.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Ficha técnica e métricas do modelo de triagem, exibidas na tela Inteligência. */
public record ModeloIaResponse(
        boolean disponivel,
        String algoritmo,
        int exemplosTreino,
        int vocabulario,
        Map<String, Integer> exemplosPorClasse,
        double limiarConfianca,
        LocalDateTime treinadoEm,
        Avaliacao validacaoCruzada) {

    /**
     * @param matriz  [classe real][classe prevista]; a última coluna conta "sem sinal"
     */
    public record Avaliacao(int folds, int exemplos, double acuracia, double f1Macro,
                            List<String> classes, int[][] matriz,
                            List<MetricaClasse> porClasse, List<PontoLimiar> curvaLimiar) {}

    public record MetricaClasse(String classe, double precisao, double revocacao, double f1, int suporte) {}

    public record PontoLimiar(double limiar, double cobertura, double acuracia) {}
}
