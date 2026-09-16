package com.test.recutement_test.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Format d'erreur standard renvoyé par l'API")
public class ErrorResponse {

    @Schema(description = "Horodatage de l'erreur (UTC)", example = "2026-09-16T21:47:47.935229Z")
    private Instant timestamp;

    @Schema(description = "Code HTTP", example = "404")
    private int status;

    @Schema(description = "Libellé du code HTTP", example = "Not Found")
    private String error;

    @Schema(description = "Message d'erreur lisible", example = "Tâche introuvable")
    private String message;

    @Schema(description = "Chemin de la requête à l'origine de l'erreur", example = "/api/tasks/42")
    private String path;

    @Schema(description = "Détail des erreurs de validation par champ, présent uniquement sur une erreur 400 "
            + "de validation", example = "{\"title\": \"Le titre est requis\"}")
    private Map<String, String> fieldErrors;
}
