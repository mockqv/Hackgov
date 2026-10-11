package br.com.hackgov.algoritmos;

/**
 * Union-Find (conjuntos disjuntos) para achar componentes conexos de um grafo
 * à medida que as arestas aparecem, sem precisar montar a lista de adjacência.
 *
 * Cada conjunto é uma árvore representada pelo vetor de pais; a raiz é o
 * "representante". Duas otimizações deixam cada operação em tempo amortizado
 * O(α(n)) — α é a inversa da função de Ackermann, que não passa de 4 para
 * qualquer n prático:
 *  - união por posto: a árvore mais baixa vira filha da mais alta;
 *  - compressão de caminho (path halving): durante o encontrar, cada nó passa
 *    a apontar para o avô, achatando a árvore.
 */
public final class UnionFind {

    private final int[] pai;
    private final byte[] posto;
    private int componentes;

    public UnionFind(int n) {
        pai = new int[n];
        posto = new byte[n];
        componentes = n;
        for (int i = 0; i < n; i++) pai[i] = i;
    }

    /** Representante (raiz) do conjunto de x. */
    public int encontrar(int x) {
        while (pai[x] != x) {
            pai[x] = pai[pai[x]];
            x = pai[x];
        }
        return x;
    }

    /** Une os conjuntos de a e b. Devolve false se já estavam juntos. */
    public boolean unir(int a, int b) {
        int ra = encontrar(a);
        int rb = encontrar(b);
        if (ra == rb) return false;
        if (posto[ra] < posto[rb]) {
            pai[ra] = rb;
        } else if (posto[ra] > posto[rb]) {
            pai[rb] = ra;
        } else {
            pai[rb] = ra;
            posto[ra]++;
        }
        componentes--;
        return true;
    }

    public boolean conectados(int a, int b) {
        return encontrar(a) == encontrar(b);
    }

    public int componentes() {
        return componentes;
    }

    public int tamanho() {
        return pai.length;
    }
}
