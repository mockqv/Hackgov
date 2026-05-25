package br.com.hackgov.dto;

import br.com.hackgov.model.StatusSolicitacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AtualizarStatusRequest {

    @NotNull(message = "Novo status é obrigatório")
    private StatusSolicitacao novoStatus;

    @Size(max = 500, message = "Justificativa muito longa")
    private String justificativa;

    @Size(max = 300, message = "Caminho da foto muito longo")
    private String caminhoFotoDepois;
}
