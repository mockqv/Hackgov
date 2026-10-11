package br.com.hackgov.dto;

import java.util.List;

/**
 * Sugestão da IA de triagem para uma descrição.
 *
 * @param disponivel       false quando o modelo não pôde ser carregado (o sistema segue sem IA)
 * @param confiavel        true quando a confiança atinge o limiar — só então a tela sugere o tipo
 * @param tipoServicoId    id do tipo no catálogo (para pré-selecionar no formulário)
 * @param codigo           código do tipo sugerido (BURACO, ILUMINACAO, ...)
 * @param tipoDescricao    descrição do tipo sugerido
 * @param confianca        probabilidade a posteriori da classe vencedora (0..1)
 * @param alternativas     as três classes mais prováveis
 * @param termosRelevantes palavras que mais pesaram na decisão (explicação)
 * @param limiar           limiar de confiança usado para considerar a sugestão confiável
 */
public record ClassificacaoResponse(
        boolean disponivel,
        boolean confiavel,
        Long tipoServicoId,
        String codigo,
        String tipoDescricao,
        double confianca,
        List<Alternativa> alternativas,
        List<String> termosRelevantes,
        double limiar) {

    public record Alternativa(String codigo, String descricao, double probabilidade) {}

    public static ClassificacaoResponse indisponivel(double limiar) {
        return new ClassificacaoResponse(false, false, null, null, null, 0, List.of(), List.of(), limiar);
    }

    public static ClassificacaoResponse semSinal(double limiar) {
        return new ClassificacaoResponse(true, false, null, null, null, 0, List.of(), List.of(), limiar);
    }
}
