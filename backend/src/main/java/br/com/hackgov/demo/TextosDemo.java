package br.com.hackgov.demo;

import java.util.List;
import java.util.Random;

/**
 * Banco de frases das descrições simuladas. É propositalmente diferente do
 * corpus de treino da IA (outras estruturas e combinações de palavras), para
 * que a massa de demonstração também sirva de teste de generalização.
 */
final class TextosDemo {

    private final Random rnd;

    TextosDemo(Random rnd) {
        this.rnd = rnd;
    }

    private static final List<String> ONDE = List.of(
            "na altura do nº 450", "em frente à padaria", "perto da escola estadual", "na faixa de ônibus",
            "no acesso ao posto de saúde", "na saída da feira livre", "ao lado do ponto de táxi",
            "na esquina da farmácia", "em frente ao condomínio", "perto da igreja", "na rua de trás do mercado",
            "na entrada da creche", "ao lado da quadra", "na frente do bar do Zé");

    // ── BURACO ───────────────────────────────────────────────
    private static final List<String> BUR_TAM = List.of("grande", "enorme", "fundo", "de uns 40 cm", "que só aumenta", "perigoso");
    private static final List<String> BUR_EFEITO = List.of(
            "os carros estão desviando pela calçada", "já furou pneu de vários carros", "uma moto caiu semana passada",
            "os motoristas freiam em cima da hora", "a água da chuva acumula e esconde o buraco",
            "o ônibus balança inteiro quando passa");
    private static final List<String> BUR_ESTADO = List.of("afundou", "cedeu", "rachou", "se desfez");

    // ── ILUMINAÇÃO ───────────────────────────────────────────
    private static final List<String> ILU_PROB = List.of(
            "apagado há dias", "piscando sem parar", "aceso o dia inteiro", "sem funcionar desde a chuva");
    private static final List<String> ILU_PROB_F = List.of(
            "queimou", "está queimada", "pisca a noite toda", "fica acesa de dia", "não acende mais");
    private static final List<String> ILU_EFEITO = List.of(
            "a rua fica muito escura", "os moradores estão com medo de assalto",
            "quem volta do trabalho anda no escuro", "o ponto de ônibus fica sem luz nenhuma");

    // ── PODA ─────────────────────────────────────────────────
    private static final List<String> POD_PROB = List.of(
            "com galhos sobre a fiação", "inclinada com risco de queda", "seca e com cupim",
            "caída depois do temporal", "com o tronco rachado");
    private static final List<String> POD_GALHO = List.of("quebrado pendurado", "enorme caído", "seco prestes a cair");
    private static final List<String> POD_EFEITO = List.of(
            "com vento pode cair em alguém", "é risco para pedestres e carros", "já derrubou o cabo de internet",
            "ninguém passa pela calçada");

    // ── ENTULHO ──────────────────────────────────────────────
    private static final List<String> ENT_OBJ = List.of(
            "entulho de obra", "restos de reforma", "móveis velhos", "um sofá e um colchão", "sacos de entulho",
            "pneus usados", "tijolos e telhas quebradas", "madeira e restos de demolição");
    private static final List<String> ENT_VERBO = List.of("Jogaram", "Descartaram", "Largaram", "Despejaram");
    private static final List<String> ENT_EFEITO = List.of(
            "virou criadouro de mosquito", "está atraindo ratos", "impede a passagem na calçada",
            "o cheiro está insuportável");

    // ── OUTROS ───────────────────────────────────────────────
    private static final List<String[]> OUT_ITENS = List.of(
            new String[]{"Calçada", "quebrada e com piso solto, idosos tropeçam"},
            new String[]{"Bueiro", "entupido, a rua alaga com qualquer chuva"},
            new String[]{"Semáforo", "com defeito, fica só no amarelo piscante"},
            new String[]{"Placa de trânsito", "derrubada no chão"},
            new String[]{"Terreno baldio", "com mato muito alto e bichos"},
            new String[]{"Vazamento de água", "na sarjeta há vários dias"},
            new String[]{"Muro da praça", "todo pichado"},
            new String[]{"Boca de lobo", "sem grade, criança pode cair"},
            new String[]{"Ponto de ônibus", "sem cobertura e com banco quebrado"},
            new String[]{"Faixa de pedestre", "apagada, ninguém respeita"},
            new String[]{"Lixeira pública", "arrancada do poste"},
            new String[]{"Brinquedo do parquinho", "quebrado com ferro exposto"});

    String descricao(String tipo) {
        String onde = um(ONDE);
        return switch (tipo) {
            case "BURACO" -> switch (rnd.nextInt(6)) {
                case 0 -> "Buraco " + um(BUR_TAM) + " " + onde + ", " + um(BUR_EFEITO) + ".";
                case 1 -> cap(um(BUR_EFEITO)) + " por causa do buraco " + onde + ".";
                case 2 -> "O asfalto " + onde + " " + um(BUR_ESTADO) + " e abriu um buraco " + um(BUR_TAM) + ".";
                case 3 -> "Solicito tapa-buraco " + onde + ". " + cap(um(BUR_EFEITO)) + ".";
                case 4 -> "buraco " + onde + " " + um(List.of("ta enorme ja", "ninguem arruma", "cuidado moto", "piorou com a chuva"));
                default -> "Pavimento danificado " + onde + ": " + um(BUR_EFEITO) + ".";
            };
            case "ILUMINACAO" -> switch (rnd.nextInt(6)) {
                case 0 -> "Poste " + onde + " " + um(ILU_PROB) + ", " + um(ILU_EFEITO) + ".";
                case 1 -> "A lâmpada do poste " + onde + " " + um(ILU_PROB_F) + ".";
                case 2 -> cap(um(ILU_EFEITO)) + ": a iluminação " + onde + " não funciona.";
                case 3 -> "Solicito troca de lâmpada " + onde + ", " + um(ILU_EFEITO) + ".";
                case 4 -> "poste " + onde + " " + um(List.of("apagado de novo", "ta escuro demais", "pisca igual arvore de natal"));
                default -> "Iluminação pública com defeito " + onde + ", " + um(ILU_EFEITO) + ".";
            };
            case "PODA" -> switch (rnd.nextInt(5)) {
                case 0 -> "Árvore " + onde + " " + um(POD_PROB) + ", " + um(POD_EFEITO) + ".";
                case 1 -> "Galho " + um(POD_GALHO) + " " + onde + ", " + um(POD_EFEITO) + ".";
                case 2 -> "Solicito poda da árvore " + onde + ": está " + um(POD_PROB) + ".";
                case 3 -> "arvore " + onde + " " + um(List.of("ta caindo", "perigo com essa chuva", "precisa podar urgente"));
                default -> cap(um(POD_EFEITO)) + ": árvore " + onde + " " + um(POD_PROB) + ".";
            };
            case "ENTULHO" -> switch (rnd.nextInt(5)) {
                case 0 -> "Descarte irregular de " + um(ENT_OBJ) + " " + onde + ", " + um(ENT_EFEITO) + ".";
                case 1 -> um(ENT_VERBO) + " " + um(ENT_OBJ) + " " + onde + ".";
                case 2 -> "Tem " + um(ENT_OBJ) + " " + onde + " há mais de uma semana, " + um(ENT_EFEITO) + ".";
                case 3 -> "Solicito remoção de " + um(ENT_OBJ) + " " + onde + ".";
                default -> "entulho " + onde + " " + um(List.of("de novo jogaram la", "virou lixao", "ninguem recolhe"));
            };
            default -> {
                String[] item = OUT_ITENS.get(rnd.nextInt(OUT_ITENS.size()));
                yield item[0] + " " + onde + " " + item[1] + ".";
            }
        };
    }

    String justificativaCancelamento(String origem) {
        if ("duplicata".equals(origem)) return "Solicitação duplicada: o mesmo problema já está em atendimento em outro protocolo.";
        return um(List.of(
                "Fora da competência municipal: encaminhado à concessionária responsável.",
                "Endereço não localizado pela equipe de vistoria.",
                "Problema já havia sido resolvido no momento da vistoria.",
                "Solicitação duplicada: o mesmo problema já está em atendimento em outro protocolo."));
    }

    String comentarioAvaliacao(int nota) {
        if (rnd.nextDouble() < 0.4) return null;
        return switch (nota) {
            case 5 -> um(List.of("Resolveram rápido, obrigado!", "Serviço muito bem feito.", "Excelente atendimento."));
            case 4 -> um(List.of("Bom serviço.", "Resolveram dentro do esperado.", "Ficou bom."));
            case 3 -> um(List.of("Resolveram, mas demorou.", "Serviço razoável.", "Poderia ter sido mais rápido."));
            case 2 -> um(List.of("Demorou demais.", "Serviço mal acabado.", "Tive que reclamar várias vezes."));
            default -> um(List.of("Prazo estourado e serviço ruim.", "Péssimo, o problema voltou.", "Muito demorado."));
        };
    }

    private String um(List<String> opcoes) {
        return opcoes.get(rnd.nextInt(opcoes.size()));
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
