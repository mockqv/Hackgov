package br.com.hackgov.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NormalizadorTextoTest {

    @Test
    @DisplayName("normalizar: minúsculas, sem acento e sem pontuação")
    void normalizaAcentosEPontuacao() {
        assertEquals("lampada queimada na praca", NormalizadorTexto.normalizar("Lâmpada QUEIMADA, na praça!!!"));
        assertEquals("", NormalizadorTexto.normalizar(null));
        assertEquals("", NormalizadorTexto.normalizar("   "));
    }

    @Test
    @DisplayName("tokens: remove stopwords, números e aplica o stemming leve de plural")
    void tokensSemStopwordsComStemming() {
        List<String> tokens = NormalizadorTexto.tokens("Os buracos da rua 25 e as luzes dos postes");
        assertEquals(List.of("buraco", "luz", "poste"), tokens);
    }

    @Test
    @DisplayName("radical: plurais comuns de chamados viram a forma singular")
    void radicalDePlurais() {
        assertEquals("arvore", NormalizadorTexto.radical("arvores"));
        assertEquals("luz", NormalizadorTexto.radical("luzes"));
        assertEquals("caminhao", NormalizadorTexto.radical("caminhoes"));
        assertEquals("movel", NormalizadorTexto.radical("moveis"));
        assertEquals("onibus", NormalizadorTexto.radical("onibus"));
        assertEquals("gas", NormalizadorTexto.radical("gas"));
    }

    @Test
    @DisplayName("características: unigramas seguidos de bigramas")
    void caracteristicasComBigramas() {
        List<String> c = NormalizadorTexto.caracteristicas("Poste apagado à noite");
        assertEquals(List.of("poste", "apagado", "noite", "poste_apagado", "apagado_noite"), c);
        assertEquals("poste apagado", NormalizadorTexto.legivel("poste_apagado"));
    }

    @Test
    @DisplayName("termos exibidos ao usuário mantêm a grafia original (com acento)")
    void legivelComGrafiaOriginal() {
        var formas = NormalizadorTexto.formasOriginais("A árvore caiu sobre a lâmpada do poste");
        assertEquals("árvore caiu", NormalizadorTexto.legivel("arvore_caiu", formas));
        assertEquals("lâmpada", NormalizadorTexto.legivel("lampada", formas));
        assertEquals("fiacao", NormalizadorTexto.legivel("fiacao", formas)); // sem forma original: usa o radical
    }

    @Test
    @DisplayName("texto só com stopwords não gera características")
    void textoSemConteudo() {
        assertTrue(NormalizadorTexto.caracteristicas("Por favor, olá, bom dia!").isEmpty());
        assertFalse(NormalizadorTexto.caracteristicas("Buraco").isEmpty());
    }
}
