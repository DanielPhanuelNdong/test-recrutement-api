package com.test.recutement_test.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Données d'inscription d'un nouvel utilisateur")
public record RegisterRequest(

        @Schema(description = "Nom complet de l'utilisateur", example = "Alice Doe")
        @NotBlank(message = "Le nom complet est requis")
        String fullName,

        @Schema(description = "Adresse email, utilisée comme identifiant de connexion", example = "alice@example.com")
        @NotBlank(message = "L'email est requis")
        @Email(message = "L'email doit être valide")
        String email,

        @Schema(description = "Mot de passe en clair (haché côté serveur), 6 caractères minimum",
                example = "password123", minLength = 6)
        @NotBlank(message = "Le mot de passe est requis")
        @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
        String password
) {
}
