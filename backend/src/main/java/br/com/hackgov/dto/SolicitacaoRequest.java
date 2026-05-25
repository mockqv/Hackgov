package br.com.hackgov.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SolicitacaoRequest {
    @NotNull(message = "ID do cidadão é obrigatório")
    private Long cidadaoId;
    @NotNull(message = "ID do tipo de serviço é obrigatório")
    private Long tipoServicoId;
    @NotBlank(message = "Descrição é obrigatória")
    @Size(min = 10, max = 500)
    private String descricao;
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;
    private String logradouro;
    private String numero;
    private String complemento;
    private String cep;
    @NotNull(message = "ID do bairro é obrigatório")
    private Long bairroId;
    private String caminhoFotoAntes;
}
