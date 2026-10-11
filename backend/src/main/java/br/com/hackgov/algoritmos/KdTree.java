package br.com.hackgov.algoritmos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

/**
 * KD-Tree bidimensional (k = 2) para busca de vizinhos dentro de um raio.
 *
 * É uma árvore binária de busca que alterna o eixo de corte a cada nível:
 * na raiz os pontos são separados pela coordenada x (mediana), no nível
 * seguinte por y, depois x de novo, e assim por diante. Cada nó guarda um
 * ponto e divide o plano em dois semiplanos.
 *
 * Representação implícita: a árvore é só uma permutação de índices. O nó do
 * intervalo [ini, fim) é o elemento do meio; a subárvore esquerda é
 * [ini, meio) e a direita [meio + 1, fim). Não há objetos de nó — memória O(n).
 *
 * Construção: em cada nível, um quickselect com partição em 3 vias coloca a
 * mediana no meio em O(n) esperado (robusto mesmo com muitas coordenadas
 * repetidas). Como a árvore tem altura ⌈log₂(n+1)⌉, a construção custa
 * O(n log n).
 *
 * Busca por raio: desce pela árvore e só entra num lado do corte se o círculo
 * de busca alcançar aquele semiplano (poda). Custo médio O(log n + k) para k
 * pontos encontrados; pior caso O(√n + k) em 2D.
 */
public final class KdTree {

    private final double[] xs;
    private final double[] ys;
    private final int[] arvore;
    private final Random aleatorio = new Random(31L);

    /** Constrói a árvore sobre os pontos (xs[i], ys[i]). Os arrays não são copiados nem alterados. */
    public KdTree(double[] xs, double[] ys) {
        if (xs.length != ys.length) throw new IllegalArgumentException("xs e ys com tamanhos diferentes");
        this.xs = xs;
        this.ys = ys;
        this.arvore = new int[xs.length];
        for (int i = 0; i < arvore.length; i++) arvore[i] = i;
        construir(0, arvore.length, 0);
    }

    public int tamanho() {
        return arvore.length;
    }

    /** Altura da árvore balanceada: ⌈log₂(n + 1)⌉. */
    public int altura() {
        int h = 0;
        for (int n = arvore.length; n > 0; n >>= 1) h++;
        return h;
    }

    /** Chama {@code acao} com o índice de cada ponto a distância ≤ raio de (qx, qy). */
    public void buscarNoRaio(double qx, double qy, double raio, IntConsumer acao) {
        buscar(0, arvore.length, 0, qx, qy, raio, raio * raio, acao);
    }

    public List<Integer> buscarNoRaio(double qx, double qy, double raio) {
        List<Integer> encontrados = new ArrayList<>();
        buscarNoRaio(qx, qy, raio, encontrados::add);
        return encontrados;
    }

    /** Quantos nós a busca visita — usado no benchmark para mostrar o efeito da poda. */
    public int nosVisitados(double qx, double qy, double raio) {
        int[] contador = {0};
        contar(0, arvore.length, 0, qx, qy, raio, contador);
        return contador[0];
    }

    // ── construção ───────────────────────────────────────────

    private void construir(int ini, int fim, int profundidade) {
        if (fim - ini <= 1) return;
        int meio = (ini + fim) >>> 1;
        int eixo = profundidade & 1;
        selecionar(ini, fim - 1, meio, eixo);
        construir(ini, meio, profundidade + 1);
        construir(meio + 1, fim, profundidade + 1);
    }

    /**
     * Quickselect com partição em 3 vias (menores | iguais | maiores): ao final,
     * arvore[k] tem a k-ésima menor coordenada do eixo dentro de [esq, dir],
     * tudo à esquerda é ≤ e tudo à direita é ≥.
     */
    private void selecionar(int esq, int dir, int k, int eixo) {
        while (esq < dir) {
            double pivo = coordenada(arvore[esq + aleatorio.nextInt(dir - esq + 1)], eixo);
            int menores = esq;
            int i = esq;
            int maiores = dir;
            while (i <= maiores) {
                double v = coordenada(arvore[i], eixo);
                if (v < pivo) {
                    trocar(menores++, i++);
                } else if (v > pivo) {
                    trocar(i, maiores--);
                } else {
                    i++;
                }
            }
            if (k < menores) {
                dir = menores - 1;
            } else if (k > maiores) {
                esq = maiores + 1;
            } else {
                return;
            }
        }
    }

    // ── busca ────────────────────────────────────────────────

    private void buscar(int ini, int fim, int profundidade, double qx, double qy,
                        double raio, double raio2, IntConsumer acao) {
        if (ini >= fim) return;
        int meio = (ini + fim) >>> 1;
        int indice = arvore[meio];
        double dx = xs[indice] - qx;
        double dy = ys[indice] - qy;
        if (dx * dx + dy * dy <= raio2) acao.accept(indice);

        int eixo = profundidade & 1;
        double q = eixo == 0 ? qx : qy;
        double corte = eixo == 0 ? xs[indice] : ys[indice];
        if (q - raio <= corte) buscar(ini, meio, profundidade + 1, qx, qy, raio, raio2, acao);
        if (q + raio >= corte) buscar(meio + 1, fim, profundidade + 1, qx, qy, raio, raio2, acao);
    }

    private void contar(int ini, int fim, int profundidade, double qx, double qy, double raio, int[] contador) {
        if (ini >= fim) return;
        contador[0]++;
        int meio = (ini + fim) >>> 1;
        int indice = arvore[meio];
        int eixo = profundidade & 1;
        double q = eixo == 0 ? qx : qy;
        double corte = eixo == 0 ? xs[indice] : ys[indice];
        if (q - raio <= corte) contar(ini, meio, profundidade + 1, qx, qy, raio, contador);
        if (q + raio >= corte) contar(meio + 1, fim, profundidade + 1, qx, qy, raio, contador);
    }

    private double coordenada(int indice, int eixo) {
        return eixo == 0 ? xs[indice] : ys[indice];
    }

    private void trocar(int a, int b) {
        int t = arvore[a];
        arvore[a] = arvore[b];
        arvore[b] = t;
    }
}
