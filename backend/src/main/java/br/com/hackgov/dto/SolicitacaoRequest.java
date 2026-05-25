package br.com.hackgov.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SolicitacaoRequest {

    @NotNull(message = "Tipo de serviço é obrigatório")
    private Long tipoServicoId;

    @NotBlank(message = "Descrição é obrigatória")
    @Size(min = 10, max = 500, message = "Descrição deve ter entre 10 e 500 caracteres")
    private String descricao;

    @NotNull(message = "Latitude é obrigatória")
    @DecimalMin(value = "-90.0",  message = "Latitude inválida")
    @DecimalMax(value = "90.0",   message = "Latitude inválida")
    private Double latitude;

    @NotNull(message = "Longitude é obrigatória")
    @DecimalMin(value = "-180.0", message = "Longitude inválida")
    @DecimalMax(value = "180.0",  message = "Longitude inválida")
    private Double longitude;

    @Size(max = 200, message = "Logradouro muito longo")
    private String logradouro;

    @Size(max = 20, message = "Número muito longo")
    private String numero;

    @Size(max = 100, message = "Complemento muito longo")
    private String complemento;

    @Pattern(regexp = "^$|\\d{8}", message = "CEP deve ter 8 dígitos numéricos")
    private String cep;

    @NotNull(message = "Bairro é obrigatório")
    private Long bairroId;

    @Size(max = 300)
    private String caminhoFotoAntes;
}
