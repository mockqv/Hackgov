package br.com.hackgov.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Texto livre do chamado a ser classificado pela IA de triagem. */
public record ClassificacaoRequest(
        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        String descricao) {}
