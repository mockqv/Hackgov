package br.com.hackgov.security;

import br.com.hackgov.model.Cidadao;
import br.com.hackgov.model.Perfil;
import br.com.hackgov.model.Servidor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class AuthenticatedUser implements UserDetails {

    public enum Kind { CIDADAO, SERVIDOR }

    private final Long id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final Perfil perfil;
    private final Kind kind;
    private final boolean ativo;

    private AuthenticatedUser(Long id, String nome, String email, String senhaHash,
                              Perfil perfil, Kind kind, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.kind = kind;
        this.ativo = ativo;
    }

    public static AuthenticatedUser fromCidadao(Cidadao c) {
        return new AuthenticatedUser(
                c.getId(), c.getNome(), c.getEmail(), c.getSenhaHash(),
                c.getPerfil(), Kind.CIDADAO, "S".equals(c.getAtivo())
        );
    }

    public static AuthenticatedUser fromServidor(Servidor s) {
        return new AuthenticatedUser(
                s.getId(), s.getNome(), s.getEmailFuncional(), s.getSenhaHash(),
                s.getPerfil(), Kind.SERVIDOR, "S".equals(s.getAtivo())
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(perfil.authority()));
    }

    @Override public String getPassword() { return senhaHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return ativo; }
    @Override public boolean isAccountNonLocked() { return ativo; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return ativo; }
}
