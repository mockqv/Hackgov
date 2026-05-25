package br.com.hackgov.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CidadaoRequest {
    @NotBlank(message = "Nome é obrigatório") @Size(max = 150)
    private String nome;
    @NotBlank @Email(message = "E-mail inválido")
    private String email;
    @NotBlank @Size(min = 11, max = 11, message = "CPF deve ter 11 dígitos")
    private String cpf;
    @Size(max = 20)
    private String telefone;
}
