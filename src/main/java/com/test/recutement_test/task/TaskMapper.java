package com.test.recutement_test.task;

import com.test.recutement_test.task.dto.TaskRequest;
import com.test.recutement_test.task.dto.TaskResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapping entité/DTO pour {@link Task}. L'affectation du propriétaire ({@code user}) et
 * la mise à jour conditionnelle du statut restent portées par {@link TaskService}, car ce
 * sont des règles métier et non de simples correspondances de champs.
 */
@Mapper(componentModel = "spring")
public interface TaskMapper {

    TaskResponse toResponse(Task task);

    List<TaskResponse> toResponseList(List<Task> tasks);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", expression = "java(request.status() != null ? request.status() : TaskStatus.TODO)")
    Task toEntity(TaskRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(TaskRequest request, @MappingTarget Task task);
}
