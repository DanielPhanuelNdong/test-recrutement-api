package com.test.recutement_test.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ne déclare volontairement pas de {@code SecurityRequirement} global : seuls les
 * contrôleurs qui exposent des endpoints protégés (ex. {@code TaskController}) portent
 * {@code @SecurityRequirement(name = "bearerAuth")}, afin que Swagger UI n'affiche pas
 * le cadenas sur les endpoints publics ({@code /api/auth/**}).
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI taskManagerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Manager API")
                        .description("""
                                API REST pour la mini application de gestion de tâches ("Task Manager").

                                Permet de créer un compte, se connecter, puis gérer (créer / lister / filtrer / \
                                modifier / supprimer) les tâches de l'utilisateur authentifié. L'API est \
                                consommée aussi bien par le frontend web (React) que par l'application mobile \
                                (Flutter), via le même contrat JWT.

                                ### Authentification
                                1. Appeler `POST /api/auth/register` ou `POST /api/auth/login` pour obtenir un `token`.
                                2. Cliquer sur **Authorize** ci-dessus et saisir `Bearer <token>`.
                                3. Les endpoints `/api/tasks/**` deviennent alors accessibles depuis cette page.
                                """)
                        .version("v1")
                        .contact(new Contact()
                                .name("Task Manager API")
                                .email("ndongphanuel69@gmail.com"))
                        .license(new License().name("Usage interne - test de recrutement")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Environnement local")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Jeton JWT obtenu via /api/auth/login ou /api/auth/register, "
                                        + "à préfixer par \"Bearer \".")));
    }
}
