package br.com.hackgov.ia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Leitura do corpus rotulado usado para treinar a IA de triagem.
 *
 * Formato (UTF-8, um exemplo por linha): {@code CLASSE;texto do chamado}.
 * Linhas vazias e linhas iniciadas com '#' são ignoradas; a primeira linha
 * pode ser o cabeçalho {@code classe;texto}.
 */
public final class CorpusTriagem {

    /** Caminho do corpus no classpath (src/main/resources/ia/corpus_triagem.csv). */
    public static final String RECURSO_PADRAO = "/ia/corpus_triagem.csv";

    private CorpusTriagem() {}

    public static List<ExemploRotulado> carregarPadrao() {
        try (InputStream in = CorpusTriagem.class.getResourceAsStream(RECURSO_PADRAO)) {
            if (in == null) throw new IllegalStateException("Corpus não encontrado no classpath: " + RECURSO_PADRAO);
            return carregar(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o corpus de triagem", e);
        }
    }

    public static List<ExemploRotulado> carregar(InputStream in) throws IOException {
        List<ExemploRotulado> exemplos = new ArrayList<>();
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String linha;
            int numero = 0;
            while ((linha = leitor.readLine()) != null) {
                numero++;
                String l = linha.strip();
                if (l.isEmpty() || l.startsWith("#") || l.equalsIgnoreCase("classe;texto")) continue;
                int separador = l.indexOf(';');
                if (separador <= 0 || separador == l.length() - 1) {
                    throw new IllegalArgumentException("Linha " + numero + " do corpus fora do formato CLASSE;texto");
                }
                exemplos.add(new ExemploRotulado(l.substring(separador + 1).strip(), l.substring(0, separador).strip()));
            }
        }
        return exemplos;
    }
}
