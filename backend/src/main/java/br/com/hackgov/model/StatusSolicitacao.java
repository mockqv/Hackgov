package br.com.hackgov.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum StatusSolicitacao {
    RECEBIDO,
    EM_ANALISE,
    AGENDADO,
    EM_EXECUCAO,
    CONCLUIDO,
    CANCELADO;

    /**
     * Transições permitidas. CONCLUIDO e CANCELADO são estados terminais.
     */
    private static final Map<StatusSolicitacao, Set<StatusSolicitacao>> TRANSICOES = Map.of(
            RECEBIDO,    EnumSet.of(EM_ANALISE, CANCELADO),
            EM_ANALISE,  EnumSet.of(AGENDADO, CANCELADO),
            AGENDADO,    EnumSet.of(EM_EXECUCAO, CANCELADO),
            EM_EXECUCAO, EnumSet.of(CONCLUIDO, CANCELADO),
            CONCLUIDO,   EnumSet.noneOf(StatusSolicitacao.class),
            CANCELADO,   EnumSet.noneOf(StatusSolicitacao.class)
    );

    public boolean podeTransicionarPara(StatusSolicitacao destino) {
        return TRANSICOES.getOrDefault(this, EnumSet.noneOf(StatusSolicitacao.class))
                .contains(destino);
    }

    public boolean isTerminal() {
        return this == CONCLUIDO || this == CANCELADO;
    }

    public String descricao() {
        return switch (this) {
            case RECEBIDO    -> "Recebido";
            case EM_ANALISE  -> "Em análise";
            case AGENDADO    -> "Agendado";
            case EM_EXECUCAO -> "Em execução";
            case CONCLUIDO   -> "Concluído";
            case CANCELADO   -> "Cancelado";
        };
    }
}
