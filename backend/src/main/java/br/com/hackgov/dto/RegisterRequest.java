package br.com.hackgov.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 200)
    private String email;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos numéricos")
    private String cpf;

    @Pattern(regexp = "^$|\\d{10,11}", message = "Telefone deve ter 10 ou 11 dígitos")
    private String telefone;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
        message = "Senha deve conter letras e números"
    )
    private String senha;
}
