package br.com.hackgov.integridade;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * Árvore de Merkle com SHA-256 e pares ordenados (Fase 7, Parte 4).
 *
 * <p>Mesma regra usada no smart contract ({@code HackGovRegistro.verificarInclusao})
 * e no script Node ({@code blockchain/lib/merkle.js}), para que uma prova gerada
 * aqui seja aceita na blockchain:
 * <ul>
 *   <li>folha = SHA-256 do texto canônico do evento, em UTF-8;</li>
 *   <li>nó = SHA-256(menor ‖ maior): ordenar o par dispensa dizer o lado na prova;</li>
 *   <li>em nível ímpar, o último nó sobe sem par.</li>
 * </ul>
 *
 * <p>Construir a árvore custa O(n); a prova de um evento tem ⌈log₂ n⌉ hashes, e
 * conferi-la custa O(log n). Um dia com 2.000 eventos precisa de 11 hashes.
 */
public final class MerkleSha256 {

    private static final HexFormat HEX = HexFormat.of();

    private MerkleSha256() {}

    public static byte[] sha256(byte[] dados) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(dados);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }

    public static byte[] folha(String textoCanonico) {
        return sha256(textoCanonico.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] par(byte[] a, byte[] b) {
        byte[] menor = Arrays.compareUnsigned(a, b) <= 0 ? a : b;
        byte[] maior = menor == a ? b : a;
        byte[] junto = new byte[menor.length + maior.length];
        System.arraycopy(menor, 0, junto, 0, menor.length);
        System.arraycopy(maior, 0, junto, menor.length, maior.length);
        return sha256(junto);
    }

    private static List<List<byte[]>> niveis(List<byte[]> folhas) {
        if (folhas == null || folhas.isEmpty()) {
            throw new IllegalArgumentException("lote sem eventos");
        }
        List<List<byte[]>> niveis = new ArrayList<>();
        niveis.add(folhas);
        while (niveis.get(niveis.size() - 1).size() > 1) {
            List<byte[]> atual = niveis.get(niveis.size() - 1);
            List<byte[]> proximo = new ArrayList<>((atual.size() + 1) / 2);
            for (int i = 0; i < atual.size(); i += 2) {
                proximo.add(i + 1 < atual.size() ? par(atual.get(i), atual.get(i + 1)) : atual.get(i));
            }
            niveis.add(proximo);
        }
        return niveis;
    }

    public static byte[] raiz(List<byte[]> folhas) {
        List<List<byte[]>> n = niveis(folhas);
        return n.get(n.size() - 1).get(0);
    }

    /** Hashes irmãos, da folha até a raiz. */
    public static List<byte[]> prova(List<byte[]> folhas, int indice) {
        if (indice < 0 || indice >= folhas.size()) {
            throw new IndexOutOfBoundsException("índice fora do lote: " + indice);
        }
        List<List<byte[]>> n = niveis(folhas);
        List<byte[]> prova = new ArrayList<>();
        int i = indice;
        for (int k = 0; k < n.size() - 1; k++) {
            int irmao = (i % 2 == 0) ? i + 1 : i - 1;
            if (irmao < n.get(k).size()) {
                prova.add(n.get(k).get(irmao));
            }
            i /= 2;
        }
        return prova;
    }

    public static boolean verificar(byte[] folha, List<byte[]> prova, byte[] raiz) {
        byte[] h = folha;
        for (byte[] p : prova) {
            h = par(h, p);
        }
        return Arrays.equals(h, raiz);
    }

    public static String hex(byte[] b) {
        return "0x" + HEX.formatHex(b);
    }

    public static byte[] deHex(String h) {
        return HEX.parseHex(h.startsWith("0x") ? h.substring(2) : h);
    }
}
