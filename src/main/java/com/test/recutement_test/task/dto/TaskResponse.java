package com.test.recutement_test.task.dto;

import com.test.recutement_test.task.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Représentation d'une tâche")
public record TaskResponse(

        @Schema(description = "Identifiant unique de la tâche", example = "1")
        Long id,

        @Schema(description = "Titre de la tâche", example = "Rédiger le README")
        String title,

        @Schema(description = "Description détaillée de la tâche",
                example = "Documenter l'installation et l'architecture du projet")
        String description,

        @Schema(description = "Statut courant de la tâche", example = "TODO")
        TaskStatus status,

        @Schema(description = "Date de création (UTC)", example = "2026-09-16T21:47:47.935229Z")
        Instant createdAt,

        @Schema(description = "Date de dernière modification (UTC)", example = "2026-09-16T21:47:47.935244Z")
        Instant updatedAt
) {
}
