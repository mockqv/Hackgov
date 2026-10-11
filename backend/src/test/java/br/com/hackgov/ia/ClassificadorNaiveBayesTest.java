package br.com.hackgov.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificadorNaiveBayesTest {

    private static final List<ExemploRotulado> MINI_CORPUS = List.of(
            new ExemploRotulado("Buraco enorme no asfalto da rua", "BURACO"),
            new ExemploRotulado("Cratera na pista, carro caiu no buraco", "BURACO"),
            new ExemploRotulado("Asfalto afundou e abriu um buraco", "BURACO"),
            new ExemploRotulado("Poste apagado, rua escura à noite", "ILUMINACAO"),
            new ExemploRotulado("Lâmpada do poste queimada", "ILUMINACAO"),
            new ExemploRotulado("Luz do poste piscando a noite toda", "ILUMINACAO"),
            new ExemploRotulado("Árvore encostando nos fios precisa de poda", "PODA"),
            new ExemploRotulado("Galho quebrado pendurado na árvore", "PODA"),
            new ExemploRotulado("Árvore seca com risco de queda", "PODA"));

    @Test
    @DisplayName("classifica textos novos na classe esperada")
    void classificaTextosNovos() {
        ClassificadorNaiveBayes modelo = ClassificadorNaiveBayes.treinado(MINI_CORPUS, 1.0);

        assertEquals("BURACO", modelo.classificar("tem um buraco fundo no asfalto").classe());
        assertEquals("ILUMINACAO", modelo.classificar("o poste da esquina está apagado").classe());
        assertEquals("PODA", modelo.classificar("galho da árvore caindo nos fios").classe());
    }

    @Test
    @DisplayName("probabilidades a posteriori somam 1 e o ranking vem ordenado")
    void probabilidadesSomamUm() {
        ClassificadorNaiveBayes modelo = ClassificadorNaiveBayes.treinado(MINI_CORPUS, 1.0);
        ResultadoClassificacao r = modelo.classificar("lâmpada queimada no poste");

        double soma = r.ranking().stream().mapToDouble(ProbabilidadeClasse::probabilidade).sum();
        assertEquals(1.0, soma, 1e-9);
        assertEquals(r.confianca(), r.ranking().get(0).probabilidade(), 1e-12);
        for (int i = 1; i < r.ranking().size(); i++) {
            assertTrue(r.ranking().get(i - 1).probabilidade() >= r.ranking().get(i).probabilidade());
        }
    }

    @Test
    @DisplayName("explica a decisão com os termos que mais pesaram")
    void explicaComTermosRelevantes() {
        ClassificadorNaiveBayes modelo = ClassificadorNaiveBayes.treinado(MINI_CORPUS, 1.0);
        ResultadoClassificacao r = modelo.classificar("poste apagado na esquina");

        assertFalse(r.termosRelevantes().isEmpty());
        assertTrue(r.termosRelevantes().contains("poste") || r.termosRelevantes().contains("apagado"));
    }

    @Test
    @DisplayName("sem nenhuma palavra conhecida não arrisca sugestão")
    void semSinalNaoSugere() {
        ClassificadorNaiveBayes modelo = ClassificadorNaiveBayes.treinado(MINI_CORPUS, 1.0);
        ResultadoClassificacao r = modelo.classificar("xyzw qwerty");

        assertTrue(r.semSinal());
        assertNull(r.classe());
        assertFalse(r.confiavel(0.0));
        assertEquals(0, r.caracteristicasReconhecidas());
    }

    @Test
    @DisplayName("aprendizado incremental: novos exemplos mudam o modelo sem retreinar tudo")
    void aprendizadoIncremental() {
        ClassificadorNaiveBayes modelo = ClassificadorNaiveBayes.treinado(MINI_CORPUS, 1.0);
        int vocabularioAntes = modelo.tamanhoVocabulario();

        modelo.adicionarExemplo("Entulho e restos de obra jogados na calçada", "ENTULHO");
        modelo.adicionarExemplo("Sofá velho e entulho abandonados no terreno", "ENTULHO");

        assertTrue(modelo.classes().contains("ENTULHO"));
        assertTrue(modelo.tamanhoVocabulario() > vocabularioAntes);
        assertEquals("ENTULHO", modelo.classificar("jogaram entulho no terreno").classe());
        assertEquals(11, modelo.totalDocumentos());
    }

    @Test
    @DisplayName("modelo sem treino e alfa inválido são rejeitados")
    void validacoes() {
        assertThrows(IllegalStateException.class, () -> new ClassificadorNaiveBayes().classificar("buraco"));
        assertThrows(IllegalArgumentException.class, () -> new ClassificadorNaiveBayes(0));
    }
}
