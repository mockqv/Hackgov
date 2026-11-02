package br.com.hackgov.estatistica;

/**
 * Correlação de Pearson e regressão linear simples por mínimos quadrados
 * (Fase 7, Parte 3), sem biblioteca externa.
 *
 * <p>Além da reta, calcula o que é preciso para decidir se a relação é real ou
 * acaso da amostra: o teste t da inclinação (H₀: β₁ = 0), com valor-p bicaudal
 * pela distribuição t de Student, e o intervalo de predição de 95% para
 * projeções. Tudo em O(n), numa passada para as médias e outra para as somas.
 */
public final class Estatistica {

    private Estatistica() {}

    /** Resultado da regressão y = intercepto + inclinação·x. */
    public record Regressao(
            int n,
            double inclinacao,
            double intercepto,
            double r,
            double r2,
            double erroPadraoResidual,
            double erroPadraoInclinacao,
            double estatisticaT,
            double valorP,
            double mediaX,
            double mediaY,
            double sxx) {

        public double prever(double x) {
            return intercepto + inclinacao * x;
        }

        /** Intervalo de predição para uma nova observação em x (ex.: 0.95). */
        public double[] intervaloPredicao(double x, double confianca) {
            double t = quantilT(1 - (1 - confianca) / 2, n - 2);
            double se = erroPadraoResidual * Math.sqrt(1 + 1.0 / n + (x - mediaX) * (x - mediaX) / sxx);
            double y = prever(x);
            return new double[]{y - t * se, y + t * se};
        }

        public boolean significativa(double alfa) {
            return valorP < alfa;
        }
    }

    public static Regressao regressao(double[] x, double[] y) {
        if (x.length != y.length) throw new IllegalArgumentException("x e y com tamanhos diferentes");
        int n = x.length;
        if (n < 3) throw new IllegalArgumentException("são necessários pelo menos 3 pontos");

        double mx = 0, my = 0;
        for (int i = 0; i < n; i++) { mx += x[i]; my += y[i]; }
        mx /= n; my /= n;

        double sxx = 0, syy = 0, sxy = 0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - mx, dy = y[i] - my;
            sxx += dx * dx; syy += dy * dy; sxy += dx * dy;
        }
        if (sxx == 0) throw new IllegalArgumentException("x constante: não há reta");

        double b1 = sxy / sxx;
        double b0 = my - b1 * mx;
        double r = syy == 0 ? 0 : sxy / Math.sqrt(sxx * syy);
        double sqRes = Math.max(0, syy - b1 * sxy);          // soma dos quadrados dos resíduos
        double s = Math.sqrt(sqRes / (n - 2));                 // erro padrão residual
        double seB1 = s / Math.sqrt(sxx);
        double t = seB1 == 0 ? Double.POSITIVE_INFINITY : b1 / seB1;
        double p = Double.isInfinite(t) ? 0 : 2 * (1 - cdfT(Math.abs(t), n - 2));
        return new Regressao(n, b1, b0, r, r * r, s, seB1, t, p, mx, my, sxx);
    }

    public static double pearson(double[] x, double[] y) {
        return regressao(x, y).r();
    }

    /** Leitura usual da força da correlação pelo |r|. */
    public static String forca(double r) {
        double a = Math.abs(r);
        if (a >= 0.9) return "muito forte";
        if (a >= 0.7) return "forte";
        if (a >= 0.4) return "moderada";
        if (a >= 0.2) return "fraca";
        return "desprezível";
    }

    // ── distribuição t de Student ────────────────────────────────

    /** F(t) da t de Student com gl graus de liberdade, via beta incompleta regularizada. */
    public static double cdfT(double t, int gl) {
        double x = gl / (gl + t * t);
        double cauda = 0.5 * betaRegularizada(gl / 2.0, 0.5, x);
        return t >= 0 ? 1 - cauda : cauda;
    }

    /** Quantil da t de Student por bisseção (p entre 0 e 1). */
    public static double quantilT(double p, int gl) {
        double lo = -1000, hi = 1000;
        for (int i = 0; i < 200; i++) {
            double meio = (lo + hi) / 2;
            if (cdfT(meio, gl) < p) lo = meio; else hi = meio;
        }
        return (lo + hi) / 2;
    }

    static double betaRegularizada(double a, double b, double x) {
        if (x <= 0) return 0;
        if (x >= 1) return 1;
        double lnFrente = lnGama(a + b) - lnGama(a) - lnGama(b) + a * Math.log(x) + b * Math.log(1 - x);
        double frente = Math.exp(lnFrente);
        // fração contínua de Lentz (Numerical Recipes, betacf); simetria para convergir rápido
        if (x < (a + 1) / (a + b + 2)) return frente * fracaoContinua(a, b, x) / a;
        return 1 - frente * fracaoContinua(b, a, 1 - x) / b;
    }

    private static double fracaoContinua(double a, double b, double x) {
        final double minimo = 1e-300, eps = 1e-15;
        double qab = a + b, qap = a + 1, qam = a - 1;
        double c = 1, d = 1 - qab * x / qap;
        if (Math.abs(d) < minimo) d = minimo;
        d = 1 / d;
        double h = d;
        for (int m = 1; m <= 300; m++) {
            int m2 = 2 * m;
            double aa = m * (b - m) * x / ((qam + m2) * (a + m2));
            d = 1 + aa * d; if (Math.abs(d) < minimo) d = minimo;
            c = 1 + aa / c; if (Math.abs(c) < minimo) c = minimo;
            d = 1 / d; h *= d * c;
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
            d = 1 + aa * d; if (Math.abs(d) < minimo) d = minimo;
            c = 1 + aa / c; if (Math.abs(c) < minimo) c = minimo;
            d = 1 / d;
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1) < eps) break;
        }
        return h;
    }

    /** ln Γ(x) pela aproximação de Lanczos (g = 7, 9 coeficientes). */
    static double lnGama(double x) {
        final double[] c = {0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
                -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6,
                1.5056327351493116e-7};
        if (x < 0.5) return Math.log(Math.PI / Math.abs(Math.sin(Math.PI * x))) - lnGama(1 - x);
        x -= 1;
        double a = c[0];
        double t = x + 7.5;
        for (int i = 1; i < 9; i++) a += c[i] / (x + i);
        return 0.5 * Math.log(2 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(a);
    }
}
