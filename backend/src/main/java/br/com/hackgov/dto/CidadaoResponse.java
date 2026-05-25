package br.com.hackgov.dto;

import br.com.hackgov.model.Cidadao;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class CidadaoResponse {
    private Long id;
    private String nome;
    private String email;
    private String telefone;
    private LocalDate dataCadastro;

    public static CidadaoResponse from(Cidadao c) {
        return CidadaoResponse.builder()
                .id(c.getId())
                .nome(c.getNome())
                .email(c.getEmail())
                .telefone(c.getTelefone())
                .dataCadastro(c.getDataCadastro())
                .build();
    }
}
