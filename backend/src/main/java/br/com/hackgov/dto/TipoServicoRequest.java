package br.com.hackgov.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload de criação/edição de um Tipo de Serviço (catálogo administrado pelo GESTOR).
 * Validado em duas camadas: aqui (Jakarta Bean Validation) e no front (zod).
 */
@Data
public class TipoServicoRequest {

    @NotBlank(message = "Código é obrigatório")
    @Size(max = 30, message = "Código deve ter no máximo 30 caracteres")
    private String codigo;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 100, message = "Descrição deve ter no máximo 100 caracteres")
    private String descricao;

    @NotNull(message = "SLA (dias) é obrigatório")
    @Min(value = 1, message = "SLA deve ser de no mínimo 1 dia")
    @Max(value = 365, message = "SLA deve ser de no máximo 365 dias")
    private Integer slaDias;
}
