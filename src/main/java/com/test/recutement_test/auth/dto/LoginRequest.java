package com.test.recutement_test.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Identifiants de connexion")
public record LoginRequest(

        @Schema(description = "Adresse email du compte", example = "alice@example.com")
        @NotBlank(message = "L'email est requis")
        @Email(message = "L'email doit être valide")
        String email,

        @Schema(description = "Mot de passe en clair", example = "password123")
        @NotBlank(message = "Le mot de passe est requis")
        String password
) {
}
