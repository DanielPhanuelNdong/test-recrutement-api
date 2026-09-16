package com.test.recutement_test.task.dto;

import com.test.recutement_test.task.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Données de création ou de modification d'une tâche")
public record TaskRequest(

        @Schema(description = "Titre de la tâche", example = "Rédiger le README", maxLength = 200)
        @NotBlank(message = "Le titre est requis")
        @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
        String title,

        @Schema(description = "Description détaillée de la tâche (optionnelle)",
                example = "Documenter l'installation et l'architecture du projet", maxLength = 2000)
        @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
        String description,

        @Schema(description = "Statut de la tâche. Par défaut TODO à la création si omis.",
                example = "TODO")
        TaskStatus status
) {
}
