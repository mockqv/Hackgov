package br.com.hackgov.controller;

import br.com.hackgov.dto.*;
import br.com.hackgov.security.AuthUtils;
import br.com.hackgov.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Cadastrar novo cidadão e retornar token")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest req) {
        AuthResponse resp = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Cadastro realizado", resp));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar por e-mail e senha")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Autenticado", authService.login(req)));
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna o usuário autenticado pelo token")
    public ResponseEntity<ApiResponse<MeResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok(MeResponse.from(AuthUtils.require())));
    }
}
