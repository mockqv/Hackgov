package br.com.hackgov.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.*;

@Configuration
public class AppConfig {

    // CORS é configurado em SecurityConfig — origens restritas via app.cors.allowed-origins.

    @Bean
    public OpenAPI openAPI() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("HackGov API")
                        .description("""
                            API REST do sistema HackGov — Zeladoria Urbana.

                            Autenticação: POST /api/auth/login retorna um JWT.
                            Enviar `Authorization: Bearer <token>` nas rotas protegidas.
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Richard Vinicius Ferreira da Silva")
                                .email("rm565628@fiap.com.br"))
                )
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
