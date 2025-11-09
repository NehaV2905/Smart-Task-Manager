package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    // Get all tasks
    public List<Task> getAllTasks() {
        return taskRepository.findAllByOrderByCreatedAtDesc();
    }

    // Get task by ID
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    // Create new task
    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    // Update task
    public Task updateTask(Long id, Task taskDetails) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        task.setTitle(taskDetails.getTitle());
        task.setDuration(taskDetails.getDuration());
        task.setCompleted(taskDetails.getCompleted());

        return taskRepository.save(task);
    }

    // Toggle task completion
    public Task toggleTaskCompletion(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        task.setCompleted(!task.getCompleted());
        return taskRepository.save(task);
    }

    // Delete task
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    // Get task statistics
    public TaskStats getTaskStats() {
        Long totalTasks = taskRepository.countBy();
        Long completedTasks = taskRepository.countByCompletedTrue();

        List<Task> allTasks = taskRepository.findAll();
        Integer totalDuration = allTasks.stream()
                .mapToInt(Task::getDuration)
                .sum();

        return new TaskStats(totalTasks, completedTasks, totalDuration);
    }

    // Inner class for statistics
    public static class TaskStats {
        private Long totalTasks;
        private Long completedTasks;
        private Integer totalDuration;

        public TaskStats(Long totalTasks, Long completedTasks, Integer totalDuration) {
            this.totalTasks = totalTasks;
            this.completedTasks = completedTasks;
            this.totalDuration = totalDuration;
        }

        // Getters
        public Long getTotalTasks() {
            return totalTasks;
        }

        public Long getCompletedTasks() {
            return completedTasks;
        }

        public Integer getTotalDuration() {
            return totalDuration;
        }
    }
}