package br.com.hackgov.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado do agrupamento espacial (KD-Tree + grafo/Union-Find).
 *
 * @param raioMetros     distância máxima entre dois chamados ligados no grafo
 * @param apenasAbertas  true = só chamados em aberto (possíveis duplicados);
 *                       false = histórico completo (pontos críticos / reincidência)
 * @param pontos         quantos chamados entraram na análise
 * @param totalGrupos    quantos grupos atingiram o tamanho mínimo
 * @param tempoMs        tempo de processamento do algoritmo
 * @param grupos         os maiores grupos (top-k), do maior para o menor
 */
public record AgrupamentosResponse(
        double raioMetros,
        boolean apenasAbertas,
        int pontos,
        int totalGrupos,
        double tempoMs,
        String algoritmo,
        List<Grupo> grupos) {

    public record Grupo(
            String codigoTipo,
            String tipoDescricao,
            int quantidade,
            int abertas,
            double latitude,
            double longitude,
            String bairro,
            String logradouro,
            LocalDateTime primeiraAbertura,
            LocalDateTime ultimaAbertura,
            List<Membro> membros) {}

    public record Membro(Long id, String protocolo, String status, LocalDateTime dataAbertura) {}
}
