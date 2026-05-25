package br.com.hackgov.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class AuthUtils {

    private AuthUtils() {}

    public static Optional<AuthenticatedUser> current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        Object principal = auth.getPrincipal();
        if (principal instanceof AuthenticatedUser u) return Optional.of(u);
        return Optional.empty();
    }

    public static AuthenticatedUser require() {
        return current().orElseThrow(() ->
                new org.springframework.security.access.AccessDeniedException("Usuário não autenticado"));
    }
}
