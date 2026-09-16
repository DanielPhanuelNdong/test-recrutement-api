package com.test.recutement_test.task;

import com.test.recutement_test.exception.ApiException;
import com.test.recutement_test.task.dto.TaskRequest;
import com.test.recutement_test.task.dto.TaskResponse;
import com.test.recutement_test.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(User currentUser, TaskStatus status, String search) {
        Specification<Task> spec = Specification.allOf(
                TaskSpecifications.belongsToUser(currentUser.getId()),
                TaskSpecifications.hasStatus(status),
                TaskSpecifications.matchesSearch(search)
        );

        List<Task> tasks = taskRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        return taskMapper.toResponseList(tasks);
    }

    @Transactional
    public TaskResponse createTask(User currentUser, TaskRequest request) {
        Task task = taskMapper.toEntity(request);
        task.setUser(currentUser);

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse updateTask(User currentUser, Long taskId, TaskRequest request) {
        Task task = findOwnedTask(currentUser, taskId);

        taskMapper.updateEntityFromRequest(request, task);
        if (request.status() != null) {
            task.setStatus(request.status());
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(User currentUser, Long taskId) {
        Task task = findOwnedTask(currentUser, taskId);
        taskRepository.delete(task);
    }

    private Task findOwnedTask(User currentUser, Long taskId) {
        return taskRepository.findByIdAndUserId(taskId, currentUser.getId())
                .orElseThrow(() -> ApiException.notFound("Tâche introuvable"));
    }
}
