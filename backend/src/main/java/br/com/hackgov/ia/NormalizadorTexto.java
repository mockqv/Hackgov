package br.com.hackgov.ia;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pré-processamento de texto em português para a IA de triagem.
 *
 * Pipeline (sempre o mesmo no treino e na predição):
 *  1. minúsculas + remoção de acentos ("Lâmpada" → "lampada");
 *  2. tudo que não é letra/dígito vira espaço (pontuação, emojis);
 *  3. remoção de stopwords e de tokens com uma letra ou só números;
 *  4. "stemming leve" de plural ("buracos" → "buraco", "luzes" → "luz");
 *  5. características = unigramas + bigramas ("poste_apagado").
 *
 * Classe sem estado e sem dependências: é usada tanto pelo serviço Spring
 * quanto pelos testes e pelo gerador da massa de demonstração.
 */
public final class NormalizadorTexto {

    /** Palavras muito frequentes que não ajudam a distinguir um tipo de serviço de outro. */
    private static final Set<String> STOPWORDS = Set.of(
            "a", "o", "e", "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas",
            "um", "uma", "uns", "umas", "para", "pra", "pro", "por", "pelo", "pela", "com",
            "que", "se", "ao", "aos", "as", "os", "ja", "mais", "muito", "muita", "muitos",
            "muitas", "esta", "estao", "estou", "esse", "essa", "este", "isso", "isto", "aqui",
            "ali", "la", "ha", "tem", "ter", "foi", "ser", "sao", "meu", "minha", "nossa",
            "nosso", "seu", "sua", "ele", "ela", "eles", "elas", "eu", "voce", "voces", "mas",
            "ou", "como", "quando", "onde", "ate", "desde", "entre", "sobre", "apos", "favor",
            "bom", "boa", "dia", "dias", "tarde", "semana", "semanas", "mes", "meses", "hoje",
            "ontem", "rua", "avenida", "av", "numero", "n", "frente", "proximo", "proxima",
            "perto", "altura", "esquina", "casa", "predio", "bairro", "moradores", "morador",
            "pessoal", "gente", "alguem", "ainda", "tambem", "so", "todo", "toda", "todos",
            "todas", "nada", "coisa", "faz", "fazer", "estava", "ficou", "fica", "vez",
            "vezes", "solicito", "solicitacao", "peco", "pedimos", "gostaria", "prefeitura",
            "providencias", "urgente", "obrigado", "obrigada", "ola", "oi", "quanto", "antes",
            "cima", "ta", "tava", "to", "vc", "pq", "pfv", "ai", "tudo"
    );

    private NormalizadorTexto() {}

    /** Minúsculas, sem acentos, só letras/dígitos separados por um espaço. */
    public static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String semAcento = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Tokens relevantes, já sem stopwords e com o stemming leve aplicado. */
    public static List<String> tokens(String texto) {
        String normalizado = normalizar(texto);
        List<String> tokens = new ArrayList<>();
        if (normalizado.isEmpty()) return tokens;
        for (String bruto : normalizado.split(" ")) {
            if (bruto.length() < 2 || STOPWORDS.contains(bruto) || bruto.chars().allMatch(Character::isDigit)) {
                continue;
            }
            String radical = radical(bruto);
            if (!STOPWORDS.contains(radical)) tokens.add(radical);
        }
        return tokens;
    }

    /** Características usadas pelo modelo: unigramas + bigramas de tokens consecutivos. */
    public static List<String> caracteristicas(String texto) {
        List<String> tokens = tokens(texto);
        List<String> caracteristicas = new ArrayList<>(tokens.size() * 2);
        caracteristicas.addAll(tokens);
        for (int i = 0; i + 1 < tokens.size(); i++) {
            caracteristicas.add(tokens.get(i) + "_" + tokens.get(i + 1));
        }
        return caracteristicas;
    }

    /** Converte uma característica interna em texto legível para a tela ("poste_apagado" → "poste apagado"). */
    public static String legivel(String caracteristica) {
        return caracteristica.replace('_', ' ');
    }

    /**
     * Como {@link #legivel(String)}, mas devolvendo cada radical na grafia em que o
     * usuário escreveu (com acento): "arvore_caiu" → "árvore caiu".
     */
    public static String legivel(String caracteristica, Map<String, String> formasOriginais) {
        String[] partes = caracteristica.split("_");
        StringBuilder sb = new StringBuilder();
        for (String parte : partes) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(formasOriginais.getOrDefault(parte, parte));
        }
        return sb.toString();
    }

    /** Radical → palavra como aparece no texto original (minúscula, acentuada). */
    public static Map<String, String> formasOriginais(String texto) {
        Map<String, String> formas = new HashMap<>();
        if (texto == null) return formas;
        for (String palavra : texto.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            String normalizada = normalizar(palavra);
            if (normalizada.isEmpty() || normalizada.contains(" ")) continue;
            formas.putIfAbsent(radical(normalizada), palavra);
        }
        return formas;
    }

    /**
     * Stemming leve, só de plural — suficiente para unificar as formas mais comuns
     * em chamados ("postes"/"poste", "arvores"/"arvore", "luzes"/"luz") sem a
     * complexidade de um stemmer completo (RSLP), que poderia agrupar demais.
     */
    static String radical(String palavra) {
        int n = palavra.length();
        if (n <= 3) return palavra;
        if (palavra.endsWith("oes") || palavra.endsWith("aes")) return palavra.substring(0, n - 3) + "ao";
        if (palavra.endsWith("zes")) return palavra.substring(0, n - 2);
        if (palavra.endsWith("ais")) return palavra.substring(0, n - 2) + "l";
        if (palavra.endsWith("eis") && n > 4) return palavra.substring(0, n - 2) + "l";
        if (palavra.endsWith("ns")) return palavra.substring(0, n - 2) + "m";
        if (palavra.endsWith("s") && !palavra.endsWith("ss") && !palavra.endsWith("us") && !palavra.endsWith("is")) {
            return palavra.substring(0, n - 1);
        }
        return palavra;
    }
}
