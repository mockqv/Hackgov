package br.com.hackgov.dto;

import br.com.hackgov.model.Perfil;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private UserSummary user;

    @Data
    @Builder
    public static class UserSummary {
        private Long id;
        private String nome;
        private String email;
        private Perfil perfil;
    }
}
