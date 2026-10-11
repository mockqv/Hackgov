package br.com.hackgov.dto;

import java.time.LocalDateTime;

/** Chamado aberto do mesmo tipo perto do ponto informado (aviso de possível duplicidade). */
public record SolicitacaoProximaResponse(
        String protocolo,
        String tipoDescricao,
        String status,
        double distanciaMetros,
        LocalDateTime dataAbertura) {}
