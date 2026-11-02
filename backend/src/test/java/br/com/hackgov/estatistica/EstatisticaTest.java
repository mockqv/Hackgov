package br.com.hackgov.estatistica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Valores de referência conferidos com scipy.stats.linregress. */
class EstatisticaTest {

    @Test
    @DisplayName("mínimos quadrados: Q(x) = 120x − 95 para (1,70) (2,120) (3,180) (4,450)")
    void retaDosMinimosQuadrados() {
        Estatistica.Regressao r = Estatistica.regressao(new double[]{1, 2, 3, 4}, new double[]{70, 120, 180, 450});
        assertEquals(120.0, r.inclinacao(), 1e-9);
        assertEquals(-95.0, r.intercepto(), 1e-9);
        assertEquals(0.9144599137, r.r(), 1e-9);
        assertEquals(37.549966711, r.erroPadraoInclinacao(), 1e-6);
        assertEquals(0.0855400863, r.valorP(), 1e-7);          // com n = 4 nem r = 0,91 é significativo
        assertFalse(r.significativa(0.05));
        assertEquals(28105.0, r.prever(235), 1e-9);
    }

    @Test
    @DisplayName("amostra maior: inclinação, r e valor-p batem com o scipy")
    void amostraMaior() {
        double[] y = {2.0037, 3.3962, 2.1776, 0.8282, 2.636, 1.5251, 5.1804, 9.5206, 4.5234, 4.6386, 8.4695, 8.5707,
                8.3162, 5.7086, 8.9122, 11.5859, 5.9674, 9.1272, 5.2963, 7.6314, 6.4748, 11.7947, 9.1977, 14.3138,
                14.4703, 13.9392, 7.4497, 13.8839, 15.8545, 16.8399};
        double[] x = new double[y.length];
        for (int i = 0; i < x.length; i++) x[i] = i;
        Estatistica.Regressao r = Estatistica.regressao(x, y);
        assertEquals(0.430943871, r.inclinacao(), 1e-8);
        assertEquals(1.759103871, r.intercepto(), 1e-8);
        assertEquals(0.84167369, r.r(), 1e-7);
        assertEquals(5.627059e-9, r.valorP(), 1e-12);
        assertTrue(r.significativa(0.05));
        assertEquals("forte", Estatistica.forca(r.r()));
    }

    @Test
    @DisplayName("distribuição t: quantil e acumulada conferem com tabelas")
    void distribuicaoT() {
        assertEquals(2.228138852, Estatistica.quantilT(0.975, 10), 1e-6);
        assertEquals(0.94903026, Estatistica.cdfT(2.0, 5), 1e-7);
        assertEquals(0.5, Estatistica.cdfT(0, 7), 1e-12);
    }

    @Test
    @DisplayName("correlação negativa perfeita, x constante e amostra pequena")
    void casosLimite() {
        Estatistica.Regressao r = Estatistica.regressao(new double[]{1, 2, 3, 4, 5}, new double[]{10, 8, 6, 4, 2});
        assertEquals(-1.0, r.r(), 1e-12);
        assertEquals(0.0, r.valorP(), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> Estatistica.regressao(new double[]{2, 2, 2}, new double[]{1, 2, 3}));
        assertThrows(IllegalArgumentException.class, () -> Estatistica.regressao(new double[]{1, 2}, new double[]{1, 2}));
    }

    @Test
    @DisplayName("intervalo de predição envolve a previsão e alarga longe da média")
    void intervaloDePredicao() {
        Estatistica.Regressao r = Estatistica.regressao(new double[]{1, 2, 3, 4, 5, 6}, new double[]{2.1, 3.9, 6.2, 7.8, 10.1, 12.2});
        double[] perto = r.intervaloPredicao(3.5, 0.95);
        double[] longe = r.intervaloPredicao(12, 0.95);
        assertTrue(perto[0] < r.prever(3.5) && r.prever(3.5) < perto[1]);
        assertTrue(longe[1] - longe[0] > perto[1] - perto[0]);
    }
}
