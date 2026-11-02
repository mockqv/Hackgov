package br.com.hackgov.integridade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerkleSha256Test {

    private static List<byte[]> folhas(int n) {
        List<byte[]> f = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            f.add(MerkleSha256.folha(i + "|ZEL-2026-B" + (i - 1) + "|RECEBIDO|EM_ANALISE|2026-10-15T10:"
                    + String.format("%02d", i - 1) + ":00|3|triagem"));
        }
        return f;
    }

    @Test
    @DisplayName("raiz idêntica à calculada pelo script Node (blockchain/lib/merkle.js)")
    void mesmaRaizQueONode() {
        // valor gerado com: node -e "const m=require('./lib/merkle');...13 eventos do teste do contrato"
        assertEquals("0x" + RAIZ_NODE_13, MerkleSha256.hex(MerkleSha256.raiz(folhas(13))));
    }

    @Test
    @DisplayName("toda folha prova inclusão, para tamanhos pares, ímpares e 1")
    void provasValidas() {
        for (int n : new int[]{1, 2, 3, 7, 8, 13, 64, 100}) {
            List<byte[]> f = folhas(n);
            byte[] raiz = MerkleSha256.raiz(f);
            for (int i = 0; i < n; i++) {
                assertTrue(MerkleSha256.verificar(f.get(i), MerkleSha256.prova(f, i), raiz), "n=" + n + " i=" + i);
            }
            assertTrue(MerkleSha256.prova(f, 0).size() <= Math.ceil(Math.log(n) / Math.log(2)));
        }
    }

    @Test
    @DisplayName("um caractere alterado num evento invalida a prova e muda a raiz")
    void detectaAdulteracao() {
        List<byte[]> f = folhas(13);
        byte[] raiz = MerkleSha256.raiz(f);
        byte[] adulterada = MerkleSha256.folha("7|ZEL-2026-B6|RECEBIDO|EM_ANALISE|2026-10-15T10:06:00|3|triagem (editado)");
        assertFalse(MerkleSha256.verificar(adulterada, MerkleSha256.prova(f, 6), raiz));
        List<byte[]> g = new ArrayList<>(f);
        g.set(6, adulterada);
        assertFalse(MerkleSha256.hex(raiz).equals(MerkleSha256.hex(MerkleSha256.raiz(g))));
    }

    @Test
    @DisplayName("texto canônico: campos fixos, data sem frações e separador escapado")
    void textoCanonico() {
        String t = RegistroCanonico.texto(42L, "ZEL-2026-1A2B3C4D", "RECEBIDO", "EM_ANALISE",
                LocalDateTime.of(2026, 10, 15, 9, 30, 12, 999_000_000), 3L, "ok | conferido");
        assertEquals("v1|42|ZEL-2026-1A2B3C4D|RECEBIDO|EM_ANALISE|2026-10-15T09:30:12|3|ok \\| conferido", t);
    }

    private static final String RAIZ_NODE_13 = "6f0f66957b4feee1180423c3aaca196c4ab494ef9d30b671fd7bce86b7f6a1ab";
}
