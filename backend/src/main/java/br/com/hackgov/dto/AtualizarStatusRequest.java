package br.com.hackgov.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AtualizarStatusRequest {
    @NotNull(message = "ID do servidor é obrigatório")
    private Long servidorId;
    @NotBlank(message = "Novo status é obrigatório")
    private String novoStatus;
    @Size(max = 500)
    private String justificativa;
    private String caminhoFotoDepois;
}
