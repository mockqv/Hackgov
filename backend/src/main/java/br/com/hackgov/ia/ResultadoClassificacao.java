package br.com.hackgov.ia;

import java.util.List;

/**
 * Saída do classificador para um texto.
 *
 * @param classe                      classe mais provável, ou {@code null} quando o texto não tem
 *                                    nenhuma característica conhecida pelo modelo (sem sinal)
 * @param confianca                   probabilidade a posteriori da classe vencedora (0..1)
 * @param ranking                     todas as classes, da mais para a menos provável
 * @param termosRelevantes            características do texto que mais pesaram a favor da classe
 *                                    vencedora — é a explicação mostrada ao usuário
 * @param caracteristicasReconhecidas quantas características do texto existiam no vocabulário
 */
public record ResultadoClassificacao(
        String classe,
        double confianca,
        List<ProbabilidadeClasse> ranking,
        List<String> termosRelevantes,
        int caracteristicasReconhecidas) {

    public boolean semSinal() {
        return classe == null;
    }

    /** Só vale como sugestão se houver sinal e a confiança atingir o limiar configurado. */
    public boolean confiavel(double limiar) {
        return classe != null && confianca >= limiar;
    }
}
