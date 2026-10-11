package br.com.hackgov.algoritmos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgrupadorEspacialTest {

    /** ≈ 1 metro em graus de latitude. */
    private static final double METRO = 1.0 / 111_195.0;
    private static final double LAT = -23.55;
    private static final double LON = -46.63;

    private static PontoGeo ponto(long id, double norteMetros, double lesteMetros, String categoria) {
        double lon = LON + lesteMetros * METRO / Math.cos(Math.toRadians(LAT));
        return new PontoGeo(id, LAT + norteMetros * METRO, lon, categoria);
    }

    @Test
    @DisplayName("distância corrigida: 1 grau de longitude em SP mede ~102 km, não 111 km")
    void distanciaCorrigida() {
        double umGrauLon = Geo.distanciaMetros(LAT, LON, LAT, LON + 1);
        assertEquals(101_950, umGrauLon, 200);
        double vinteMetros = Geo.distanciaMetros(LAT, LON, LAT + 20 * METRO, LON);
        assertEquals(20.0, vinteMetros, 0.05);
    }

    @Test
    @DisplayName("componentes conexos: cadeia de vizinhos vira um grupo; categorias não se misturam")
    void cenarioControlado() {
        List<PontoGeo> pontos = List.of(
                ponto(1, 0, 0, "BURACO"),
                ponto(2, 30, 0, "BURACO"),     // 30 m do 1
                ponto(3, 60, 0, "BURACO"),     // 30 m do 2 (60 m do 1): entra por encadeamento
                ponto(4, 400, 0, "BURACO"),    // isolado
                ponto(5, 0, 5, "ILUMINACAO"),  // perto do 1, mas outra categoria
                ponto(6, 0, 15, "ILUMINACAO"));

        List<AgrupadorEspacial.Grupo> grupos = AgrupadorEspacial.agrupar(pontos, 40, 2);

        assertEquals(2, grupos.size());
        assertEquals(Set.of(1L, 2L, 3L), ids(grupos.get(0)));
        assertEquals("BURACO", grupos.get(0).categoria());
        assertEquals(Set.of(5L, 6L), ids(grupos.get(1)));
        assertEquals(LAT + 30 * METRO, grupos.get(0).latitudeCentro(), 1e-9);
    }

    @Test
    @DisplayName("KD-Tree + Union-Find produz exatamente os mesmos grupos da força bruta")
    void equivaleAForcaBruta() {
        Random rnd = new Random(99);
        String[] categorias = {"BURACO", "ILUMINACAO", "PODA", "ENTULHO", "OUTROS"};
        List<PontoGeo> pontos = new ArrayList<>();
        for (int i = 0; i < 4_000; i++) {
            double norte = rnd.nextDouble() < 0.3 ? rnd.nextInt(20) * 150 + rnd.nextGaussian() * 25 : rnd.nextDouble() * 6_000;
            double leste = rnd.nextDouble() < 0.3 ? rnd.nextInt(20) * 150 + rnd.nextGaussian() * 25 : rnd.nextDouble() * 6_000;
            pontos.add(ponto(i, norte, leste, categorias[rnd.nextInt(categorias.length)]));
        }

        Set<Set<Long>> rapido = assinatura(AgrupadorEspacial.agrupar(pontos, 50, 2));
        Set<Set<Long>> referencia = assinatura(AgrupadorEspacial.agruparForcaBruta(pontos, 50, 2));

        assertTrue(rapido.size() > 50, "cenário deveria gerar vários grupos");
        assertEquals(referencia, rapido);
    }

    @Test
    @DisplayName("top-k com heap devolve os maiores grupos em ordem decrescente")
    void topK() {
        List<PontoGeo> pontos = new ArrayList<>();
        long id = 1;
        int[] tamanhos = {2, 7, 3, 5, 4};
        for (int g = 0; g < tamanhos.length; g++) {
            for (int k = 0; k < tamanhos[g]; k++) pontos.add(ponto(id++, g * 1_000, k * 10, "BURACO"));
        }
        List<AgrupadorEspacial.Grupo> todos = AgrupadorEspacial.agrupar(pontos, 15, 2);
        List<AgrupadorEspacial.Grupo> top3 = AgrupadorEspacial.maiores(todos, 3);

        assertEquals(List.of(7, 5, 4), top3.stream().map(AgrupadorEspacial.Grupo::tamanho).toList());
        assertEquals(5, AgrupadorEspacial.maiores(todos, 10).size());
        assertEquals(7, AgrupadorEspacial.indicePorId(todos).get(4L).tamanho());
    }

    private static Set<Long> ids(AgrupadorEspacial.Grupo g) {
        return g.membros().stream().map(PontoGeo::id).collect(Collectors.toSet());
    }

    private static Set<Set<Long>> assinatura(List<AgrupadorEspacial.Grupo> grupos) {
        Set<Set<Long>> s = new HashSet<>();
        for (AgrupadorEspacial.Grupo g : grupos) s.add(ids(g));
        return s;
    }
}
