package br.com.hackgov.ia;

import java.util.List;

/**
 * Métricas de qualidade do classificador, calculadas sobre exemplos que o modelo
 * NÃO viu no treino (validação cruzada ou conjunto de teste separado).
 *
 * @param exemplos        quantidade de exemplos avaliados
 * @param folds           k da validação cruzada (0 quando é um conjunto de teste fixo)
 * @param acuracia        acertos / total
 * @param f1Macro         média simples do F1 de cada classe (não favorece a classe maior)
 * @param classes         ordem das linhas/colunas da matriz de confusão
 * @param matrizConfusao  [classe real][classe prevista]; a última coluna conta "sem sinal"
 * @param porClasse       precisão, revocação, F1 e suporte de cada classe
 * @param curvaLimiar     cobertura × acurácia para diferentes limiares de confiança
 * @param erros           amostra dos erros (texto, real, previsto) para análise qualitativa
 */
public record AvaliacaoModelo(
        int exemplos,
        int folds,
        double acuracia,
        double f1Macro,
        List<String> classes,
        int[][] matrizConfusao,
        List<MetricaClasse> porClasse,
        List<PontoLimiar> curvaLimiar,
        List<ErroClassificacao> erros) {

    /** Métricas de uma classe. */
    public record MetricaClasse(String classe, double precisao, double revocacao, double f1, int suporte) {}

    /**
     * Para um limiar τ: cobertura = fração dos textos em que a IA faz sugestão (confiança ≥ τ);
     * acurácia = acertos entre os textos cobertos.
     */
    public record PontoLimiar(double limiar, double cobertura, double acuracia) {}

    /** Um erro do modelo, guardado para análise (ex.: "semáforo apagado" confundido com iluminação). */
    public record ErroClassificacao(String texto, String classeReal, String classePrevista, double confianca) {}
}
