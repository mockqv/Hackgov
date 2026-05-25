package br.com.hackgov.config;

import br.com.hackgov.model.*;
import br.com.hackgov.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * Popula dados básicos quando rodando em profile "dev":
 *  - Tipos de serviço (Buraco, Iluminação, Poda, Entulho, Outros)
 *  - 1 bairro padrão ("Centro / São Paulo / SP")
 *  - 1 servidor de teste (servidor@hackgov.local / Hackgov@123)
 *  - 1 gestor   de teste (gestor@hackgov.local   / Hackgov@123)
 *  - 1 cidadão  de teste (cidadao@hackgov.local  / Hackgov@123)
 *
 * Idempotente: só cria o que não existir. Não toca em produção.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

    private static final String SENHA_PADRAO = "Hackgov@123";

    private final TipoServicoRepository tipoServicoRepo;
    private final BairroRepository      bairroRepo;
    private final CidadaoRepository     cidadaoRepo;
    private final ServidorRepository    servidorRepo;
    private final PasswordEncoder       passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("===== DevDataSeeder iniciando (profile=dev) =====");
        seedTiposServico();
        seedBairro();
        seedCidadaoDemo();
        seedServidor();
        seedGestor();
        log.info("===== DevDataSeeder concluído =====");
        log.info("Login de teste: senha padrão = {}", SENHA_PADRAO);
        log.info("  cidadao@hackgov.local   (CIDADAO)");
        log.info("  servidor@hackgov.local  (SERVIDOR)");
        log.info("  gestor@hackgov.local    (GESTOR)");
    }

    // ── TIPOS DE SERVIÇO ────────────────────────────────────
    private void seedTiposServico() {
        record TS(String codigo, String descricao, int sla) {}
        List<TS> tipos = List.of(
            new TS("BURACO",      "Buraco na via",        7),
            new TS("ILUMINACAO",  "Iluminação queimada",  5),
            new TS("PODA",        "Poda de árvore",      14),
            new TS("ENTULHO",     "Remoção de entulho",   7),
            new TS("OUTROS",      "Outros",              10)
        );

        for (TS t : tipos) {
            tipoServicoRepo.findAll().stream()
                .filter(existente -> existente.getCodigo().equalsIgnoreCase(t.codigo()))
                .findAny()
                .ifPresentOrElse(
                    e -> {},
                    () -> {
                        tipoServicoRepo.save(TipoServico.builder()
                            .codigo(t.codigo())
                            .descricao(t.descricao())
                            .slaDias(t.sla())
                            .ativo("S")
                            .build());
                        log.info("  + tipo de serviço criado: {}", t.descricao());
                    });
        }
    }

    // ── BAIRRO PADRÃO ───────────────────────────────────────
    private void seedBairro() {
        if (bairroRepo.count() > 0) return;
        Bairro b = bairroRepo.save(Bairro.builder()
            .nomeBairro("Centro")
            .municipio("São Paulo")
            .uf("SP")
            .build());
        log.info("  + bairro padrão criado: id={} {}/{}/{}",
                b.getId(), b.getNomeBairro(), b.getMunicipio(), b.getUf());
    }

    // ── USUÁRIOS DE TESTE ───────────────────────────────────
    private void seedCidadaoDemo() {
        String email = "cidadao@hackgov.local";
        if (cidadaoRepo.findByEmail(email).isPresent()) return;
        cidadaoRepo.save(Cidadao.builder()
            .nome("Cidadão de Teste")
            .email(email)
            .cpfHash(sha256("00000000191"))      // CPF fictício válido
            .telefone("11999990000")
            .senhaHash(passwordEncoder.encode(SENHA_PADRAO))
            .perfil(Perfil.CIDADAO)
            .build());
        log.info("  + cidadão de teste criado: {}", email);
    }

    private void seedServidor() {
        String email = "servidor@hackgov.local";
        if (servidorRepo.findByEmailFuncional(email).isPresent()) return;
        servidorRepo.save(Servidor.builder()
            .nome("Servidor de Teste")
            .matricula("SRV001")
            .emailFuncional(email)
            .cargo("Analista de Zeladoria")
            .senhaHash(passwordEncoder.encode(SENHA_PADRAO))
            .perfil(Perfil.SERVIDOR)
            .ativo("S")
            .build());
        log.info("  + servidor de teste criado: {}", email);
    }

    private void seedGestor() {
        String email = "gestor@hackgov.local";
        if (servidorRepo.findByEmailFuncional(email).isPresent()) return;
        servidorRepo.save(Servidor.builder()
            .nome("Gestor de Teste")
            .matricula("GST001")
            .emailFuncional(email)
            .cargo("Coordenador")
            .senhaHash(passwordEncoder.encode(SENHA_PADRAO))
            .perfil(Perfil.GESTOR)
            .ativo("S")
            .build());
        log.info("  + gestor de teste criado: {}", email);
    }

    // ── util ────────────────────────────────────────────────
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Falha gerando SHA-256", e);
        }
    }
}
