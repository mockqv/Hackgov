package br.com.hackgov.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.*;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class AppConfig {

    // ── CORS — libera o front-end (ajuste a origem em produção) ──
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }

    // ── Swagger / OpenAPI 3 ───────────────────────────────────
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HackGov API")
                        .description("""
                            API REST do sistema HackGov — Zeladoria Urbana.
                            
                            Fluxo do cidadão: POST /cidadaos → POST /solicitacoes → GET /solicitacoes/protocolo/{p} → POST /solicitacoes/{id}/avaliar
                            
                            Fluxo do servidor: GET /solicitacoes/abertas → PATCH /solicitacoes/{id}/status
                            
                            Dashboard: GET /dashboard
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Richard Vinicius Ferreira da Silva")
                                .email("rm565628@fiap.com.br"))
                );
    }
}
