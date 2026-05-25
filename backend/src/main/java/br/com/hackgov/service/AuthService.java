package br.com.hackgov.service;

import br.com.hackgov.dto.AuthResponse;
import br.com.hackgov.dto.LoginRequest;
import br.com.hackgov.dto.RegisterRequest;
import br.com.hackgov.exception.BusinessException;
import br.com.hackgov.model.Cidadao;
import br.com.hackgov.model.Perfil;
import br.com.hackgov.repository.CidadaoRepository;
import br.com.hackgov.repository.ServidorRepository;
import br.com.hackgov.security.AuthenticatedUser;
import br.com.hackgov.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final CidadaoRepository cidadaoRepo;
    private final ServidorRepository servidorRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest req) {
        String emailNorm = req.getEmail().trim().toLowerCase();

        if (cidadaoRepo.findByEmail(emailNorm).isPresent()
                || servidorRepo.findByEmailFuncional(emailNorm).isPresent()) {
            throw new BusinessException("E-mail já cadastrado");
        }

        String cpfHash = sha256(req.getCpf());

        Cidadao novo = Cidadao.builder()
                .nome(req.getNome().trim())
                .email(emailNorm)
                .cpfHash(cpfHash)
                .telefone(req.getTelefone())
                .senhaHash(passwordEncoder.encode(req.getSenha()))
                .perfil(Perfil.CIDADAO)
                .build();

        Cidadao salvo = cidadaoRepo.save(novo);
        log.info("Cidadão registrado: id={} email={}", salvo.getId(), salvo.getEmail());

        AuthenticatedUser principal = AuthenticatedUser.fromCidadao(salvo);
        return buildAuth(principal);
    }

    public AuthResponse login(LoginRequest req) {
        String emailNorm = req.getEmail().trim().toLowerCase();
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(emailNorm, req.getSenha())
        );
        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        log.info("Login: email={} perfil={}", principal.getEmail(), principal.getPerfil());
        return buildAuth(principal);
    }

    private AuthResponse buildAuth(AuthenticatedUser user) {
        String token = jwtService.generate(user);
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .nome(user.getNome())
                        .email(user.getEmail())
                        .perfil(user.getPerfil())
                        .build())
                .build();
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar hash SHA-256", e);
        }
    }
}
