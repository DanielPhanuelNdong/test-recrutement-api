package com.test.recutement_test.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse renvoyée après une inscription ou une connexion réussie")
public record AuthResponse(

        @Schema(description = "Jeton JWT à utiliser dans l'en-tête Authorization",
                example = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhbGljZUBleGFtcGxlLmNvbSJ9.signature")
        String token,

        @Schema(description = "Type de jeton, à préfixer devant le token", example = "Bearer")
        String tokenType,

        @Schema(description = "Durée de validité du jeton, en secondes", example = "86400")
        Long expiresIn,

        @Schema(description = "Identifiant de l'utilisateur", example = "1")
        Long userId,

        @Schema(description = "Email de l'utilisateur connecté", example = "alice@example.com")
        String email,

        @Schema(description = "Nom complet de l'utilisateur connecté", example = "Alice Doe")
        String fullName
) {
}
