package com.test.recutement_test.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.test.recutement_test.task.dto.TaskRequest;
import com.test.recutement_test.task.dto.TaskResponse;
import com.test.recutement_test.user.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TaskMapperTest {

    private final TaskMapper mapper = new TaskMapperImpl();

    @Test
    void toResponseMapsAllFields() {
        User owner = User.builder().id(1L).email("alice@example.com").build();
        Task task = Task.builder()
                .id(42L)
                .title("Write tests")
                .description("Cover the mapper layer")
                .status(TaskStatus.IN_PROGRESS)
                .user(owner)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .build();

        TaskResponse response = mapper.toResponse(task);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.title()).isEqualTo("Write tests");
        assertThat(response.description()).isEqualTo("Cover the mapper layer");
        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    void toEntityDefaultsStatusToTodoWhenAbsent() {
        TaskRequest request = new TaskRequest("New task", "desc", null);

        Task task = mapper.toEntity(request);

        assertThat(task.getTitle()).isEqualTo("New task");
        assertThat(task.getDescription()).isEqualTo("desc");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getId()).isNull();
        assertThat(task.getUser()).isNull();
        assertThat(task.getCreatedAt()).isNull();
        assertThat(task.getUpdatedAt()).isNull();
    }

    @Test
    void toEntityKeepsExplicitStatus() {
        TaskRequest request = new TaskRequest("New task", "desc", TaskStatus.DONE);

        Task task = mapper.toEntity(request);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void updateEntityFromRequestLeavesOwnershipAndStatusUntouched() {
        User owner = User.builder().id(1L).build();
        Task task = Task.builder()
                .id(7L)
                .title("Old title")
                .description("Old description")
                .status(TaskStatus.IN_PROGRESS)
                .user(owner)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();

        TaskRequest request = new TaskRequest("New title", "New description", TaskStatus.DONE);
        mapper.updateEntityFromRequest(request, task);

        // title/description are simple field mappings handled by MapStruct.
        assertThat(task.getTitle()).isEqualTo("New title");
        assertThat(task.getDescription()).isEqualTo("New description");
        // id, user, createdAt, updatedAt and status are business rules owned by TaskService,
        // the mapper must never touch them.
        assertThat(task.getId()).isEqualTo(7L);
        assertThat(task.getUser()).isSameAs(owner);
        assertThat(task.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(task.getUpdatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }
}
