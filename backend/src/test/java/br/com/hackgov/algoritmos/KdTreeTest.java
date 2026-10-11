package br.com.hackgov.algoritmos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KdTreeTest {

    @Test
    @DisplayName("busca por raio devolve exatamente os mesmos pontos que a força bruta")
    void equivaleAForcaBruta() {
        Random rnd = new Random(2026);
        int n = 3_000;
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            // metade espalhada numa área de 5 km × 5 km, metade concentrada em 5 focos
            if (i % 2 == 0) {
                xs[i] = rnd.nextDouble() * 5_000;
                ys[i] = rnd.nextDouble() * 5_000;
            } else {
                int foco = rnd.nextInt(5);
                xs[i] = 1_000 * foco + rnd.nextGaussian() * 40;
                ys[i] = 800 * foco + rnd.nextGaussian() * 40;
            }
        }
        KdTree arvore = new KdTree(xs, ys);

        for (int q = 0; q < 300; q++) {
            double qx = rnd.nextDouble() * 5_000;
            double qy = rnd.nextDouble() * 5_000;
            double raio = 10 + rnd.nextDouble() * 400;
            Set<Integer> esperado = new HashSet<>();
            for (int i = 0; i < n; i++) {
                double dx = xs[i] - qx;
                double dy = ys[i] - qy;
                if (dx * dx + dy * dy <= raio * raio) esperado.add(i);
            }
            assertEquals(esperado, new HashSet<>(arvore.buscarNoRaio(qx, qy, raio)));
        }
    }

    @Test
    @DisplayName("coordenadas repetidas não quebram a construção nem a busca")
    void coordenadasRepetidas() {
        int n = 5_000;
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = i % 3;      // só 3 valores distintos de x
            ys[i] = 7.0;        // y sempre igual
        }
        KdTree arvore = new KdTree(xs, ys);

        assertEquals(n, arvore.buscarNoRaio(1, 7, 1.0).size());
        assertEquals(n / 3 + (n % 3 > 0 ? 1 : 0), arvore.buscarNoRaio(0, 7, 0.5).size());
    }

    @Test
    @DisplayName("altura logarítmica e poda: a busca visita uma fração pequena dos nós")
    void alturaEPoda() {
        Random rnd = new Random(1);
        int n = 50_000;
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = rnd.nextDouble() * 30_000;
            ys[i] = rnd.nextDouble() * 30_000;
        }
        KdTree arvore = new KdTree(xs, ys);

        assertEquals(16, arvore.altura()); // ⌈log2(50.001)⌉
        int visitados = arvore.nosVisitados(15_000, 15_000, 50);
        assertTrue(visitados < n / 100, "poda ineficiente: visitou " + visitados + " de " + n);
    }

    @Test
    @DisplayName("árvore vazia e de um ponto")
    void casosLimite() {
        assertEquals(List.of(), new KdTree(new double[0], new double[0]).buscarNoRaio(0, 0, 10));
        KdTree um = new KdTree(new double[]{5}, new double[]{5});
        assertEquals(List.of(0), um.buscarNoRaio(5, 6, 1.0));
        assertEquals(List.of(), um.buscarNoRaio(5, 6.5, 1.0));
    }
}
