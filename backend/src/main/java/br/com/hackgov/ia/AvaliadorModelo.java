package br.com.hackgov.ia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Avaliação honesta do classificador: o modelo é sempre medido em exemplos que
 * ficaram de fora do treino.
 *
 * Validação cruzada estratificada (k-fold): o corpus é dividido em k partes
 * mantendo a proporção de cada classe; treina-se k vezes com k−1 partes e
 * testa-se na parte restante. Assim todo exemplo é testado exatamente uma vez.
 */
public final class AvaliadorModelo {

    /** Limiares exibidos na curva cobertura × acurácia. */
    public static final double[] LIMIARES = {0.0, 0.5, 0.6, 0.7, 0.8, 0.9};

    private static final int MAX_ERROS_GUARDADOS = 60;

    private AvaliadorModelo() {}

    public static AvaliacaoModelo validacaoCruzada(List<ExemploRotulado> exemplos, int k, long semente, double alfa) {
        if (k < 2) throw new IllegalArgumentException("k deve ser pelo menos 2");

        Map<String, List<ExemploRotulado>> porClasse = new TreeMap<>();
        for (ExemploRotulado e : exemplos) {
            porClasse.computeIfAbsent(e.classe(), c -> new ArrayList<>()).add(e);
        }

        Random aleatorio = new Random(semente);
        List<List<ExemploRotulado>> partes = new ArrayList<>();
        for (int i = 0; i < k; i++) partes.add(new ArrayList<>());
        for (List<ExemploRotulado> daClasse : porClasse.values()) {
            List<ExemploRotulado> embaralhados = new ArrayList<>(daClasse);
            Collections.shuffle(embaralhados, aleatorio);
            for (int i = 0; i < embaralhados.size(); i++) {
                partes.get(i % k).add(embaralhados.get(i));
            }
        }

        List<Predicao> predicoes = new ArrayList<>(exemplos.size());
        for (int teste = 0; teste < k; teste++) {
            List<ExemploRotulado> treino = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                if (i != teste) treino.addAll(partes.get(i));
            }
            ClassificadorNaiveBayes modelo = ClassificadorNaiveBayes.treinado(treino, alfa);
            for (ExemploRotulado e : partes.get(teste)) {
                predicoes.add(prever(modelo, e));
            }
        }
        return calcular(predicoes, new ArrayList<>(porClasse.keySet()), k);
    }

    /** Avalia um modelo já treinado num conjunto de teste independente. */
    public static AvaliacaoModelo avaliar(ClassificadorNaiveBayes modelo, List<ExemploRotulado> teste) {
        List<Predicao> predicoes = new ArrayList<>(teste.size());
        TreeSet<String> classes = new TreeSet<>(modelo.classes());
        for (ExemploRotulado e : teste) {
            predicoes.add(prever(modelo, e));
            classes.add(e.classe());
        }
        return calcular(predicoes, new ArrayList<>(classes), 0);
    }

    private static Predicao prever(ClassificadorNaiveBayes modelo, ExemploRotulado e) {
        ResultadoClassificacao r = modelo.classificar(e.texto());
        return new Predicao(e.texto(), e.classe(), r.classe(), r.confianca());
    }

    static AvaliacaoModelo calcular(List<Predicao> predicoes, List<String> classes, int folds) {
        int c = classes.size();
        int[][] matriz = new int[c][c + 1]; // coluna extra = "sem sinal"
        int acertos = 0;
        List<AvaliacaoModelo.ErroClassificacao> erros = new ArrayList<>();

        for (Predicao p : predicoes) {
            int real = classes.indexOf(p.real());
            int previsto = p.previsto() == null ? c : classes.indexOf(p.previsto());
            if (real < 0) continue;
            if (previsto < 0) previsto = c;
            matriz[real][previsto]++;
            if (real == previsto) {
                acertos++;
            } else if (erros.size() < MAX_ERROS_GUARDADOS) {
                erros.add(new AvaliacaoModelo.ErroClassificacao(p.texto(), p.real(), p.previsto(), p.confianca()));
            }
        }

        List<AvaliacaoModelo.MetricaClasse> porClasse = new ArrayList<>();
        double somaF1 = 0;
        for (int i = 0; i < c; i++) {
            int vp = matriz[i][i];
            int suporte = 0;
            for (int j = 0; j <= c; j++) suporte += matriz[i][j];
            int previstosComoI = 0;
            for (int r = 0; r < c; r++) previstosComoI += matriz[r][i];
            double precisao = previstosComoI == 0 ? 0 : vp / (double) previstosComoI;
            double revocacao = suporte == 0 ? 0 : vp / (double) suporte;
            double f1 = (precisao + revocacao) == 0 ? 0 : 2 * precisao * revocacao / (precisao + revocacao);
            somaF1 += f1;
            porClasse.add(new AvaliacaoModelo.MetricaClasse(classes.get(i), precisao, revocacao, f1, suporte));
        }

        List<AvaliacaoModelo.PontoLimiar> curva = new ArrayList<>();
        for (double limiar : LIMIARES) {
            int cobertos = 0;
            int acertosCobertos = 0;
            for (Predicao p : predicoes) {
                if (p.previsto() == null || p.confianca() < limiar) continue;
                cobertos++;
                if (p.previsto().equals(p.real())) acertosCobertos++;
            }
            double cobertura = predicoes.isEmpty() ? 0 : cobertos / (double) predicoes.size();
            double acuracia = cobertos == 0 ? 0 : acertosCobertos / (double) cobertos;
            curva.add(new AvaliacaoModelo.PontoLimiar(limiar, cobertura, acuracia));
        }

        double acuracia = predicoes.isEmpty() ? 0 : acertos / (double) predicoes.size();
        return new AvaliacaoModelo(predicoes.size(), folds, acuracia, c == 0 ? 0 : somaF1 / c,
                List.copyOf(classes), matriz, List.copyOf(porClasse), List.copyOf(curva), List.copyOf(erros));
    }

    /** Uma predição individual (uso interno da avaliação). */
    record Predicao(String texto, String real, String previsto, double confianca) {}
}
