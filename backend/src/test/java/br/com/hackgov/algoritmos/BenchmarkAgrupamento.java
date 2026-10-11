package br.com.hackgov.algoritmos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Benchmark: força bruta O(n²) × KD-Tree + Union-Find O(n log n) para agrupar
 * ocorrências num raio de 50 m. Não é um teste automatizado (demora alguns
 * segundos); para rodar depois de {@code mvn test-compile}:
 *
 * <pre>
 * java -cp target/classes:target/test-classes br.com.hackgov.algoritmos.BenchmarkAgrupamento
 * </pre>
 *
 * Os pontos imitam a distribuição de chamados numa cidade: 70% concentrados em
 * torno de 12 centros de bairro e 30% espalhados por uma área de ~35 km × 40 km.
 */
public final class BenchmarkAgrupamento {

    private static final String[] CATEGORIAS = {"BURACO", "ILUMINACAO", "PODA", "ENTULHO", "OUTROS"};
    private static final double[] PESOS = {0.30, 0.24, 0.16, 0.18, 0.12};
    private static final double RAIO = 50;

    private BenchmarkAgrupamento() {}

    public static void main(String[] args) {
        int[] tamanhos = args.length > 0
                ? Arrays.stream(args).mapToInt(Integer::parseInt).toArray()
                : new int[]{1_000, 2_000, 5_000, 10_000, 20_000, 50_000, 100_000};

        // aquecimento da JIT antes de medir
        List<PontoGeo> aquecimento = gerar(5_000, 7);
        for (int i = 0; i < 5; i++) {
            AgrupadorEspacial.agrupar(aquecimento, RAIO, 2);
            AgrupadorEspacial.agruparForcaBruta(aquecimento, RAIO, 2);
        }

        System.out.println("n;forca_bruta_ms;kdtree_ms;aceleracao;grupos;iguais;construcao_ms;nos_visitados_medio;altura");
        for (int n : tamanhos) {
            List<PontoGeo> pontos = gerar(n, 2026);
            double kd = medianaMs(() -> AgrupadorEspacial.agrupar(pontos, RAIO, 2), 5);
            double bruta = medianaMs(() -> AgrupadorEspacial.agruparForcaBruta(pontos, RAIO, 2), n >= 50_000 ? 1 : 3);
            int grupos = AgrupadorEspacial.agrupar(pontos, RAIO, 2).size();
            boolean iguais = grupos == AgrupadorEspacial.agruparForcaBruta(pontos, RAIO, 2).size();

            // construção isolada e efeito da poda, na projeção plana de todos os pontos
            Geo.ProjecaoLocal proj = Geo.ProjecaoLocal.centradaEm(pontos);
            double[] xs = new double[n];
            double[] ys = new double[n];
            for (int i = 0; i < n; i++) {
                xs[i] = proj.x(pontos.get(i).longitude());
                ys[i] = proj.y(pontos.get(i).latitude());
            }
            double construcao = medianaMs(() -> new KdTree(xs, ys), 5);
            KdTree arvore = new KdTree(xs, ys);
            long visitados = 0;
            for (int i = 0; i < n; i += Math.max(1, n / 1_000)) visitados += arvore.nosVisitados(xs[i], ys[i], RAIO);
            int consultas = (n + Math.max(1, n / 1_000) - 1) / Math.max(1, n / 1_000);

            System.out.printf(Locale.ROOT, "%d;%.2f;%.2f;%.1f;%d;%s;%.2f;%.1f;%d%n",
                    n, bruta, kd, bruta / kd, grupos, iguais, construcao, visitados / (double) consultas, arvore.altura());
        }
    }

    static List<PontoGeo> gerar(int n, long semente) {
        Random rnd = new Random(semente);
        double[][] centros = new double[12][2];
        for (double[] c : centros) {
            c[0] = -23.70 + rnd.nextDouble() * 0.25;
            c[1] = -46.78 + rnd.nextDouble() * 0.33;
        }
        List<PontoGeo> pontos = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double lat;
            double lon;
            if (rnd.nextDouble() < 0.7) {
                double[] c = centros[rnd.nextInt(centros.length)];
                lat = c[0] + rnd.nextGaussian() * 0.006;   // ~650 m
                lon = c[1] + rnd.nextGaussian() * 0.006;
            } else {
                lat = -23.75 + rnd.nextDouble() * 0.32;
                lon = -46.80 + rnd.nextDouble() * 0.40;
            }
            pontos.add(new PontoGeo(i, lat, lon, sortearCategoria(rnd)));
        }
        return pontos;
    }

    private static String sortearCategoria(Random rnd) {
        double r = rnd.nextDouble();
        double acumulado = 0;
        for (int i = 0; i < CATEGORIAS.length; i++) {
            acumulado += PESOS[i];
            if (r < acumulado) return CATEGORIAS[i];
        }
        return CATEGORIAS[CATEGORIAS.length - 1];
    }

    private static double medianaMs(Runnable tarefa, int repeticoes) {
        double[] tempos = new double[repeticoes];
        for (int i = 0; i < repeticoes; i++) {
            long t0 = System.nanoTime();
            tarefa.run();
            tempos[i] = (System.nanoTime() - t0) / 1e6;
        }
        Arrays.sort(tempos);
        return tempos[repeticoes / 2];
    }
}
