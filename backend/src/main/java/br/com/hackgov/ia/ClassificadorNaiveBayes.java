package br.com.hackgov.ia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Classificador Naive Bayes multinomial para texto curto em português.
 *
 * Para cada tipo de serviço c, estima-se a partir do corpus rotulado:
 *  - P(c)     = documentos da classe / total de documentos              (a priori)
 *  - P(t | c) = (ocorrências de t em c + α) / (características de c + α·|V|)
 *    — suavização de Laplace (α = 1) para que um termo nunca visto numa classe
 *    não zere a probabilidade do texto inteiro.
 *
 * Na predição: log P(c | texto) ∝ log P(c) + Σ log P(t | c), somando em log para
 * não estourar a precisão do double, e normalizando com log-sum-exp (softmax)
 * para obter probabilidades que somam 1.
 *
 * Explicabilidade: os "termos relevantes" são as características do texto com
 * maior contraste log P(t | vencedora) − média de log P(t | demais classes).
 *
 * Uso concorrente: depois de treinado, {@link #classificar(String)} só lê as
 * estruturas — é seguro chamá-lo de várias threads. Retreino deve criar uma
 * nova instância e trocar a referência, nunca alterar a que está em uso.
 *
 * Complexidade: treino O(N·L) e predição O(C·L), com N documentos, L
 * características por texto e C classes — milissegundos para o corpus atual.
 */
public final class ClassificadorNaiveBayes {

    private static final int LIMITE_TERMOS_RELEVANTES = 4;

    private final double alfa;
    private final Map<String, Integer> documentosPorClasse = new TreeMap<>();
    private final Map<String, Map<String, Integer>> frequencias = new HashMap<>();
    private final Map<String, Integer> totalCaracteristicasPorClasse = new HashMap<>();
    private final Set<String> vocabulario = new HashSet<>();
    private int totalDocumentos;

    public ClassificadorNaiveBayes() {
        this(1.0);
    }

    public ClassificadorNaiveBayes(double alfa) {
        if (alfa <= 0) throw new IllegalArgumentException("alfa deve ser maior que zero");
        this.alfa = alfa;
    }

    public static ClassificadorNaiveBayes treinado(Collection<ExemploRotulado> exemplos, double alfa) {
        ClassificadorNaiveBayes modelo = new ClassificadorNaiveBayes(alfa);
        modelo.treinar(exemplos);
        return modelo;
    }

    public void treinar(Collection<ExemploRotulado> exemplos) {
        for (ExemploRotulado exemplo : exemplos) {
            adicionarExemplo(exemplo.texto(), exemplo.classe());
        }
    }

    /** Aprendizado incremental: cada exemplo novo só soma contagens (não precisa reprocessar o corpus). */
    public void adicionarExemplo(String texto, String classe) {
        documentosPorClasse.merge(classe, 1, Integer::sum);
        totalDocumentos++;
        Map<String, Integer> freq = frequencias.computeIfAbsent(classe, k -> new HashMap<>());
        for (String caracteristica : NormalizadorTexto.caracteristicas(texto)) {
            freq.merge(caracteristica, 1, Integer::sum);
            totalCaracteristicasPorClasse.merge(classe, 1, Integer::sum);
            vocabulario.add(caracteristica);
        }
    }

    public ResultadoClassificacao classificar(String texto) {
        if (totalDocumentos == 0) throw new IllegalStateException("Modelo ainda não foi treinado");

        List<String> conhecidas = new ArrayList<>();
        for (String caracteristica : NormalizadorTexto.caracteristicas(texto)) {
            if (vocabulario.contains(caracteristica)) conhecidas.add(caracteristica);
        }
        List<String> classes = new ArrayList<>(documentosPorClasse.keySet());

        // Nenhuma palavra conhecida: devolver só a priori seria um "chute" — melhor não sugerir nada.
        if (conhecidas.isEmpty()) {
            List<ProbabilidadeClasse> priori = new ArrayList<>();
            for (String classe : classes) {
                priori.add(new ProbabilidadeClasse(classe, documentosPorClasse.get(classe) / (double) totalDocumentos));
            }
            priori.sort(Comparator.comparingDouble(ProbabilidadeClasse::probabilidade).reversed());
            return new ResultadoClassificacao(null, 0.0, priori, List.of(), 0);
        }

        double[] logPosteriori = new double[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            String classe = classes.get(i);
            double soma = Math.log(documentosPorClasse.get(classe) / (double) totalDocumentos);
            for (String caracteristica : conhecidas) {
                soma += logVerossimilhanca(caracteristica, classe);
            }
            logPosteriori[i] = soma;
        }

        // log-sum-exp: subtrai o máximo antes do exp para não ter underflow
        double maximo = Double.NEGATIVE_INFINITY;
        for (double v : logPosteriori) maximo = Math.max(maximo, v);
        double somaExp = 0;
        for (double v : logPosteriori) somaExp += Math.exp(v - maximo);

        List<ProbabilidadeClasse> ranking = new ArrayList<>(classes.size());
        for (int i = 0; i < classes.size(); i++) {
            ranking.add(new ProbabilidadeClasse(classes.get(i), Math.exp(logPosteriori[i] - maximo) / somaExp));
        }
        ranking.sort(Comparator.comparingDouble(ProbabilidadeClasse::probabilidade).reversed());

        ProbabilidadeClasse vencedora = ranking.get(0);
        List<String> termos = termosRelevantes(conhecidas, vencedora.classe(), classes);
        return new ResultadoClassificacao(vencedora.classe(), vencedora.probabilidade(),
                Collections.unmodifiableList(ranking), termos, conhecidas.size());
    }

    /** log P(característica | classe) com suavização de Laplace. */
    double logVerossimilhanca(String caracteristica, String classe) {
        int contagem = frequencias.getOrDefault(classe, Map.of()).getOrDefault(caracteristica, 0);
        int total = totalCaracteristicasPorClasse.getOrDefault(classe, 0);
        return Math.log((contagem + alfa) / (total + alfa * vocabulario.size()));
    }

    private List<String> termosRelevantes(List<String> conhecidas, String vencedora, List<String> classes) {
        Map<String, Double> contraste = new LinkedHashMap<>();
        for (String caracteristica : new LinkedHashSet<>(conhecidas)) {
            double naVencedora = logVerossimilhanca(caracteristica, vencedora);
            double somaOutras = 0;
            int outras = 0;
            for (String classe : classes) {
                if (classe.equals(vencedora)) continue;
                somaOutras += logVerossimilhanca(caracteristica, classe);
                outras++;
            }
            double valor = outras == 0 ? 0 : naVencedora - somaOutras / outras;
            if (valor > 0) contraste.put(caracteristica, valor);
        }
        return contraste.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(LIMITE_TERMOS_RELEVANTES)
                .map(e -> NormalizadorTexto.legivel(e.getKey()))
                .toList();
    }

    public List<String> classes() {
        return List.copyOf(documentosPorClasse.keySet());
    }

    public int totalDocumentos() {
        return totalDocumentos;
    }

    public int tamanhoVocabulario() {
        return vocabulario.size();
    }

    public double alfa() {
        return alfa;
    }

    /** Quantos exemplos de treino existem por classe (para exibir o equilíbrio do corpus). */
    public Map<String, Integer> documentosPorClasse() {
        return Collections.unmodifiableMap(documentosPorClasse);
    }
}
