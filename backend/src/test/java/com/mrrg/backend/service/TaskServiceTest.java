package com.mrrg.backend.service;

import com.mrrg.backend.model.Task;
import com.mrrg.backend.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void findAll_shouldReturnAllTasks() {
        Task first = new Task("First task", false);
        first.setId(1L);

        Task second = new Task("Second task", true);
        second.setId(2L);

        when(taskRepository.findAll()).thenReturn(List.of(first, second));

        List<Task> result = taskService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("First task");
        assertThat(result.get(1).isCompleted()).isTrue();

        verify(taskRepository).findAll();
    }

    @Test
    void create_shouldSaveTask() {
        Task task = new Task("New task", false);

        when(taskRepository.save(task)).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Task result = taskService.create(task);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("New task");
        assertThat(result.isCompleted()).isFalse();

        verify(taskRepository).save(task);
    }

    @Test
    void update_shouldUpdateExistingTask() {
        Task existing = new Task("Old title", false);
        existing.setId(1L);

        Task update = new Task("Updated title", true);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(existing)).thenReturn(existing);

        Task result = taskService.update(1L, update);

        assertThat(result.getTitle()).isEqualTo("Updated title");
        assertThat(result.isCompleted()).isTrue();

        verify(taskRepository).findById(1L);
        verify(taskRepository).save(existing);
    }

    @Test
    void update_shouldThrowNotFound_whenTaskDoesNotExist() {
        Task update = new Task("Updated title", true);

        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(99L, update))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void delete_shouldDeleteTask_whenTaskExists() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.delete(1L);

        verify(taskRepository).existsById(1L);
        verify(taskRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFound_whenTaskDoesNotExist() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.delete(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verify(taskRepository, never()).deleteById(anyLong());
    }
}