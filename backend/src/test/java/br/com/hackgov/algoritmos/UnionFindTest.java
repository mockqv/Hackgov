package br.com.hackgov.algoritmos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnionFindTest {

    @Test
    @DisplayName("une conjuntos e conta componentes conexos")
    void uneEContaComponentes() {
        UnionFind uf = new UnionFind(6);
        assertEquals(6, uf.componentes());

        assertTrue(uf.unir(0, 1));
        assertTrue(uf.unir(1, 2));
        assertTrue(uf.unir(4, 5));
        assertFalse(uf.unir(0, 2), "0 e 2 já estavam no mesmo conjunto");

        assertEquals(3, uf.componentes());            // {0,1,2} {3} {4,5}
        assertTrue(uf.conectados(0, 2));
        assertFalse(uf.conectados(2, 3));
        assertEquals(uf.encontrar(4), uf.encontrar(5));
    }

    @Test
    @DisplayName("cadeia longa: compressão de caminho mantém o mesmo representante")
    void cadeiaLonga() {
        int n = 100_000;
        UnionFind uf = new UnionFind(n);
        for (int i = 1; i < n; i++) uf.unir(i - 1, i);

        assertEquals(1, uf.componentes());
        int raiz = uf.encontrar(0);
        for (int i = 0; i < n; i += 997) assertEquals(raiz, uf.encontrar(i));
    }
}
