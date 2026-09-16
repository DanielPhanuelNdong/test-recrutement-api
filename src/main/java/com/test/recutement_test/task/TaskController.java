package com.test.recutement_test.task;

import com.test.recutement_test.config.OpenApiConfig;
import com.test.recutement_test.exception.ErrorResponse;
import com.test.recutement_test.task.dto.TaskRequest;
import com.test.recutement_test.task.dto.TaskResponse;
import com.test.recutement_test.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tâches", description = "Gestion des tâches de l'utilisateur authentifié (CRUD, filtrage par statut, recherche texte). Nécessite un JWT valide.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Lister les tâches", description = "Renvoie les tâches de l'utilisateur connecté, triées par date de création décroissante. "
            + "Les paramètres `status` et `search` sont optionnels et combinables.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des tâches",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<TaskResponse>> getTasks(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Filtrer par statut exact") @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Recherche insensible à la casse dans le titre et la description") @RequestParam(required = false) String search) {
        return ResponseEntity.ok(taskService.getTasks(currentUser, status, search));
    }

    @PostMapping
    @Operation(summary = "Créer une tâche", description = "Crée une nouvelle tâche pour l'utilisateur connecté. Le statut par défaut est `TODO` si omis.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tâche créée",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erreur de validation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TaskResponse> createTask(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(currentUser, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une tâche", description = "Met à jour le titre, la description et le statut d'une tâche appartenant à l'utilisateur connecté.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tâche mise à jour",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TaskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Erreur de validation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tâche introuvable ou n'appartenant pas à l'utilisateur connecté",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TaskResponse> updateTask(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Identifiant de la tâche") @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(currentUser, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une tâche", description = "Supprime définitivement une tâche appartenant à l'utilisateur connecté.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tâche supprimée"),
            @ApiResponse(responseCode = "401", description = "Authentification manquante ou invalide",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Tâche introuvable ou n'appartenant pas à l'utilisateur connecté",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteTask(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Identifiant de la tâche") @PathVariable Long id) {
        taskService.deleteTask(currentUser, id);
        return ResponseEntity.noContent().build();
    }
}
