package br.com.hackgov.algoritmos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Agrupa ocorrências próximas do mesmo tipo — base para sinalizar possíveis
 * chamados duplicados e para encontrar pontos críticos (reincidência).
 *
 * Modelagem como grafo: cada solicitação é um vértice; existe aresta entre
 * duas solicitações da mesma categoria a no máximo {@code raio} metros uma da
 * outra. Os agrupamentos são os componentes conexos desse grafo (o que, em
 * clusterização, se chama ligação simples / single-linkage).
 *
 * Duas implementações com o MESMO resultado:
 *  - {@link #agruparForcaBruta}: compara todos os pares — O(n²);
 *  - {@link #agrupar}: KD-Tree para achar os vizinhos de cada ponto e
 *    Union-Find para juntar os componentes — O(n log n) em média.
 */
public final class AgrupadorEspacial {

    /** Ordem do ranking: maior grupo primeiro; empates pela categoria e pelo menor id. */
    public static final Comparator<Grupo> MAIOR_PRIMEIRO = Comparator
            .comparingInt(Grupo::tamanho).reversed()
            .thenComparing(Grupo::categoria)
            .thenComparingLong(g -> g.membros().get(0).id());

    private AgrupadorEspacial() {}

    /** Um componente conexo: ocorrências da mesma categoria ligadas por distâncias ≤ raio. */
    public record Grupo(String categoria, List<PontoGeo> membros, double latitudeCentro, double longitudeCentro) {
        public int tamanho() {
            return membros.size();
        }
    }

    /** Versão eficiente: KD-Tree (busca por raio) + Union-Find (componentes conexos). */
    public static List<Grupo> agrupar(List<PontoGeo> pontos, double raioMetros, int tamanhoMinimo) {
        return executar(pontos, raioMetros, tamanhoMinimo, true);
    }

    /** Versão de referência, comparando todos os pares. Usada nos testes e no benchmark. */
    public static List<Grupo> agruparForcaBruta(List<PontoGeo> pontos, double raioMetros, int tamanhoMinimo) {
        return executar(pontos, raioMetros, tamanhoMinimo, false);
    }

    /**
     * Os k maiores grupos, do maior para o menor, usando um heap de mínimo de
     * tamanho k: O(G log k) em vez de ordenar todos os G grupos (O(G log G)).
     */
    public static List<Grupo> maiores(List<Grupo> grupos, int k) {
        if (k <= 0) return List.of();
        PriorityQueue<Grupo> heap = new PriorityQueue<>(k + 1, MAIOR_PRIMEIRO.reversed());
        for (Grupo g : grupos) {
            heap.offer(g);
            if (heap.size() > k) heap.poll(); // descarta o menor dos k+1
        }
        List<Grupo> resultado = new ArrayList<>(heap);
        resultado.sort(MAIOR_PRIMEIRO);
        return resultado;
    }

    private static List<Grupo> executar(List<PontoGeo> pontos, double raioMetros, int tamanhoMinimo, boolean usarKdTree) {
        if (raioMetros <= 0) throw new IllegalArgumentException("raio deve ser positivo");
        if (pontos.isEmpty()) return List.of();

        Geo.ProjecaoLocal projecao = Geo.ProjecaoLocal.centradaEm(pontos);
        Map<String, List<PontoGeo>> porCategoria = new TreeMap<>();
        for (PontoGeo p : pontos) {
            porCategoria.computeIfAbsent(p.categoria(), c -> new ArrayList<>()).add(p);
        }

        List<Grupo> grupos = new ArrayList<>();
        for (Map.Entry<String, List<PontoGeo>> entrada : porCategoria.entrySet()) {
            List<PontoGeo> daCategoria = entrada.getValue();
            int n = daCategoria.size();
            double[] xs = new double[n];
            double[] ys = new double[n];
            for (int i = 0; i < n; i++) {
                xs[i] = projecao.x(daCategoria.get(i).longitude());
                ys[i] = projecao.y(daCategoria.get(i).latitude());
            }

            UnionFind componentes = new UnionFind(n);
            if (usarKdTree) {
                KdTree arvore = new KdTree(xs, ys);
                for (int i = 0; i < n; i++) {
                    final int origem = i;
                    arvore.buscarNoRaio(xs[i], ys[i], raioMetros, vizinho -> {
                        if (vizinho > origem) componentes.unir(origem, vizinho);
                    });
                }
            } else {
                double raio2 = raioMetros * raioMetros;
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        double dx = xs[i] - xs[j];
                        double dy = ys[i] - ys[j];
                        if (dx * dx + dy * dy <= raio2) componentes.unir(i, j);
                    }
                }
            }

            Map<Integer, List<PontoGeo>> porRaiz = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) {
                porRaiz.computeIfAbsent(componentes.encontrar(i), r -> new ArrayList<>()).add(daCategoria.get(i));
            }
            for (List<PontoGeo> membros : porRaiz.values()) {
                if (membros.size() < tamanhoMinimo) continue;
                membros.sort(Comparator.comparingLong(PontoGeo::id));
                double lat = 0;
                double lon = 0;
                for (PontoGeo m : membros) {
                    lat += m.latitude();
                    lon += m.longitude();
                }
                grupos.add(new Grupo(entrada.getKey(), List.copyOf(membros), lat / membros.size(), lon / membros.size()));
            }
        }
        grupos.sort(Comparator.comparingLong(g -> g.membros().get(0).id()));
        return grupos;
    }

    /** Mapa id da solicitação → grupo a que pertence (para marcar "possível duplicado" na tela). */
    public static Map<Long, Grupo> indicePorId(List<Grupo> grupos) {
        Map<Long, Grupo> indice = new HashMap<>();
        for (Grupo g : grupos) {
            for (PontoGeo p : g.membros()) indice.put(p.id(), g);
        }
        return indice;
    }
}
