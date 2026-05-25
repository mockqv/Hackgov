package br.com.hackgov.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AvaliacaoRequest {
    @NotNull @Min(1) @Max(5)
    private Integer nota;
    @Size(max = 500)
    private String comentario;
}
