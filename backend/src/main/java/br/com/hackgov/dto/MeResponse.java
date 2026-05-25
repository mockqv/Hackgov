package br.com.hackgov.dto;

import br.com.hackgov.model.Perfil;
import br.com.hackgov.security.AuthenticatedUser;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeResponse {

    private Long id;
    private String nome;
    private String email;
    private Perfil perfil;

    public static MeResponse from(AuthenticatedUser u) {
        return MeResponse.builder()
                .id(u.getId())
                .nome(u.getNome())
                .email(u.getEmail())
                .perfil(u.getPerfil())
                .build();
    }
}
