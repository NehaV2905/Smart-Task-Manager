package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.service.TaskService.TaskStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskService, using Mockito to mock the TaskRepository dependency.
 */
@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    // Mock dependency
    @Mock
    private TaskRepository taskRepository;

    // Inject mocks into the service class being tested
    @InjectMocks
    private TaskService taskService;

    private Task task1;
    private Task task2;
    private List<Task> taskList;

    /**
     * Set up common mock Task objects before each test.
     */
    @BeforeEach
    void setUp() {
        // Helper method to create tasks for consistency
        task1 = createMockTask(1L, "Task 1", 30, false);
        task2 = createMockTask(2L, "Task 2", 60, true);

        taskList = Arrays.asList(task1, task2);
    }

    /**
     * Helper to create a Task object without triggering constructor logic related to LocalDateTime.
     * This makes test setup more predictable.
     */
    private Task createMockTask(Long id, String title, Integer duration, boolean completed) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setDuration(duration);
        task.setCompleted(completed);
        task.setCreatedAt(LocalDateTime.now());
        task.setCompletedAt(completed ? LocalDateTime.now().minusHours(1) : null);
        return task;
    }

    @Test
    void getAllTasks_shouldReturnAllTasksOrdered() {
        // Arrange
        // Simulate repository returning a list of tasks ordered by creation date
        when(taskRepository.findAllByOrderByCreatedAtDesc()).thenReturn(taskList);

        // Act
        List<Task> result = taskService.getAllTasks();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(task1.getTitle(), result.get(0).getTitle());
        // Verify that the specific find method was called once
        verify(taskRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getTaskById_shouldReturnTask_whenFound() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task1));

        // Act
        Optional<Task> result = taskService.getTaskById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(task1.getTitle(), result.get().getTitle());
        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    void getTaskById_shouldReturnEmptyOptional_whenNotFound() {
        // Arrange
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        Optional<Task> result = taskService.getTaskById(99L);

        // Assert
        assertTrue(result.isEmpty());
        verify(taskRepository, times(1)).findById(99L);
    }

    @Test
    void createTask_shouldSaveAndReturnTask() {
        // Arrange
        Task newTask = new Task("New Task", 45);
        // Simulate the repository returning the saved task (with ID set)
        when(taskRepository.save(newTask)).thenReturn(task1);

        // Act
        Task createdTask = taskService.createTask(newTask);

        // Assert
        assertNotNull(createdTask);
        assertEquals(task1.getId(), createdTask.getId());
        verify(taskRepository, times(1)).save(newTask);
    }

    @Test
    void updateTask_shouldUpdateAndReturnTask_whenFound() {
        // Arrange
        Task existingTask = createMockTask(1L, "Old Title", 30, false);
        Task updateDetails = createMockTask(null, "New Title", 50, true); // Details to apply

        // Mock findById to return the existing task
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));

        // Mock save to return the modified task (using the Answer to return the passed argument)
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Task updatedTask = taskService.updateTask(1L, updateDetails);

        // Assert
        assertNotNull(updatedTask);
        assertEquals(1L, updatedTask.getId()); // ID should remain the same
        assertEquals("New Title", updatedTask.getTitle());
        assertEquals(50, updatedTask.getDuration());
        assertTrue(updatedTask.getCompleted());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(existingTask);
    }

    @Test
    void updateTask_shouldThrowException_whenNotFound() {
        // Arrange
        Task updateDetails = createMockTask(null, "New Title", 50, true);
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.updateTask(99L, updateDetails);
        });

        assertEquals("Task not found with id: 99", exception.getMessage());
        verify(taskRepository, times(1)).findById(99L);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void toggleTaskCompletion_shouldToggleToCompleted() {
        // Arrange
        // Initial state is NOT completed (false)
        Task pendingTask = createMockTask(1L, "Pending Task", 30, false);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(pendingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Task toggledTask = taskService.toggleTaskCompletion(1L);

        // Assert
        assertTrue(toggledTask.getCompleted());
        // completedAt should be set by the model's setCompleted logic
        assertNotNull(toggledTask.getCompletedAt());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(pendingTask);
    }

    @Test
    void toggleTaskCompletion_shouldToggleToPending() {
        // Arrange
        // Initial state is completed (true)
        Task completedTask = createMockTask(2L, "Completed Task", 60, true);
        when(taskRepository.findById(2L)).thenReturn(Optional.of(completedTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Task toggledTask = taskService.toggleTaskCompletion(2L);

        // Assert
        assertFalse(toggledTask.getCompleted());
        // completedAt should be reset to null by the model's setCompleted logic
        assertNull(toggledTask.getCompletedAt());
        verify(taskRepository, times(1)).findById(2L);
        verify(taskRepository, times(1)).save(completedTask);
    }

    @Test
    void deleteTask_shouldCallRepositoryDelete() {
        // Arrange
        Long taskId = 10L;

        // Act
        taskService.deleteTask(taskId);

        // Assert
        // Verify that the delete method on the repository was called once with the correct ID
        verify(taskRepository, times(1)).deleteById(taskId);
    }

    @Test
    void getTaskStats_shouldReturnCorrectStatistics() {
        // Arrange
        Task task3 = createMockTask(3L, "Task 3", 10, true);
        // Task 1 (30, false), Task 2 (60, true), Task 3 (10, true)
        List<Task> allTasks = Arrays.asList(task1, task2, task3);

        // Expected values
        // Total Tasks: 3
        // Completed Tasks: 2
        // Total Duration: 30 + 60 + 10 = 100

        when(taskRepository.countBy()).thenReturn(3L);
        when(taskRepository.countByCompletedTrue()).thenReturn(2L);
        when(taskRepository.findAll()).thenReturn(allTasks);

        // Act
        TaskStats stats = taskService.getTaskStats();

        // Assert
        assertNotNull(stats);
        assertEquals(3L, stats.getTotalTasks());
        assertEquals(2L, stats.getCompletedTasks());
        assertEquals(100, stats.getTotalDuration());

        // Verify repository methods were called
        verify(taskRepository, times(1)).countBy();
        verify(taskRepository, times(1)).countByCompletedTrue();
        verify(taskRepository, times(1)).findAll();
    }
}
