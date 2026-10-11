package br.com.hackgov.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvaliadorModeloTest {

    private static final List<ExemploRotulado> CORPUS = CorpusTriagem.carregarPadrao();

    @Test
    @DisplayName("corpus padrão: 5 classes equilibradas e pelo menos 500 exemplos")
    void corpusPadrao() {
        Map<String, Long> porClasse = CORPUS.stream()
                .collect(Collectors.groupingBy(ExemploRotulado::classe, Collectors.counting()));

        assertTrue(CORPUS.size() >= 500, "corpus pequeno demais: " + CORPUS.size());
        assertEquals(List.of("BURACO", "ENTULHO", "ILUMINACAO", "OUTROS", "PODA"),
                porClasse.keySet().stream().sorted().toList());
        porClasse.values().forEach(n -> assertTrue(n >= 100, "classe com poucos exemplos: " + n));
    }

    @Test
    @DisplayName("validação cruzada 5-fold: todo exemplo testado uma vez e acurácia alta")
    void validacaoCruzada() {
        AvaliacaoModelo av = AvaliadorModelo.validacaoCruzada(CORPUS, 5, 42L, 1.0);

        int somaMatriz = Arrays.stream(av.matrizConfusao()).flatMapToInt(Arrays::stream).sum();
        assertEquals(CORPUS.size(), av.exemplos());
        assertEquals(CORPUS.size(), somaMatriz);
        assertTrue(av.acuracia() > 0.85, "acurácia abaixo do esperado: " + av.acuracia());
        assertTrue(av.f1Macro() > 0.85, "F1 macro abaixo do esperado: " + av.f1Macro());
    }

    @Test
    @DisplayName("mesma semente → mesmo resultado (avaliação reprodutível)")
    void reprodutivel() {
        AvaliacaoModelo a = AvaliadorModelo.validacaoCruzada(CORPUS, 5, 7L, 1.0);
        AvaliacaoModelo b = AvaliadorModelo.validacaoCruzada(CORPUS, 5, 7L, 1.0);

        assertEquals(a.acuracia(), b.acuracia(), 0.0);
        assertTrue(Arrays.deepEquals(a.matrizConfusao(), b.matrizConfusao()));
    }

    @Test
    @DisplayName("conjunto independente escrito à mão (nunca visto no treino): acurácia ≥ 85%")
    void conjuntoIndependente() throws IOException {
        List<ExemploRotulado> teste;
        try (InputStream in = getClass().getResourceAsStream("/ia/teste_independente.csv")) {
            teste = CorpusTriagem.carregar(in);
        }
        ClassificadorNaiveBayes modelo = ClassificadorNaiveBayes.treinado(CORPUS, 1.0);
        AvaliacaoModelo av = AvaliadorModelo.avaliar(modelo, teste);

        assertEquals(60, av.exemplos());
        assertTrue(av.acuracia() >= 0.85, "acurácia fora do corpus: " + av.acuracia());
    }

    @Test
    @DisplayName("curva de limiar: subir o limiar reduz a cobertura e não piora a acurácia")
    void curvaDeLimiar() {
        AvaliacaoModelo av = AvaliadorModelo.validacaoCruzada(CORPUS, 5, 42L, 1.0);
        List<AvaliacaoModelo.PontoLimiar> curva = av.curvaLimiar();

        for (int i = 1; i < curva.size(); i++) {
            assertTrue(curva.get(i).cobertura() <= curva.get(i - 1).cobertura());
        }
        AvaliacaoModelo.PontoLimiar semLimiar = curva.get(0);
        AvaliacaoModelo.PontoLimiar limiar07 = curva.stream().filter(p -> p.limiar() == 0.7).findFirst().orElseThrow();
        assertTrue(limiar07.acuracia() >= semLimiar.acuracia());
    }
}
