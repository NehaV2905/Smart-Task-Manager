package com.taskmanager.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the Task model class.
 * Focuses on testing constructors, getters, setters, and the logic
 * surrounding the 'completed' and 'completedAt' fields.
 */
public class TaskTest {

    private static final String DEFAULT_TITLE = "Test Task";
    private static final int DEFAULT_DURATION = 30;

    @Test
    void defaultConstructor_shouldInitializeCreatedAt() {
        Task task = new Task();
        // Check that createdAt is initialized and is close to the time of object creation
        assertNotNull(task.getCreatedAt());
        assertFalse(task.getCompleted());
        assertNull(task.getCompletedAt());
    }

    @Test
    void parameterizedConstructor_shouldInitializeFields() {
        // Use a time slightly before object creation for comparison
        LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);

        Task task = new Task(DEFAULT_TITLE, DEFAULT_DURATION);

        assertEquals(DEFAULT_TITLE, task.getTitle());
        assertEquals(DEFAULT_DURATION, task.getDuration());
        assertFalse(task.getCompleted());
        assertNotNull(task.getCreatedAt());
        assertNull(task.getCompletedAt());

        // Ensure createdAt is roughly correct
        assertTrue(task.getCreatedAt().isAfter(beforeCreation));
    }

    @Test
    void setters_shouldWorkCorrectly() {
        Task task = new Task();
        Long taskId = 10L;
        String newTitle = "New Title";
        Integer newDuration = 60;
        LocalDateTime testTime = LocalDateTime.of(2025, 10, 31, 10, 0);

        // Test ID
        task.setId(taskId);
        assertEquals(taskId, task.getId());

        // Test Title
        task.setTitle(newTitle);
        assertEquals(newTitle, task.getTitle());

        // Test Duration
        task.setDuration(newDuration);
        assertEquals(newDuration, task.getDuration());

        // Test CreatedAt (though usually managed by constructor/JPA)
        task.setCreatedAt(testTime);
        assertEquals(testTime, task.getCreatedAt());

        // Test CompletedAt (though usually managed by setCompleted)
        task.setCompletedAt(testTime.plusHours(1));
        assertEquals(testTime.plusHours(1), task.getCompletedAt());
    }

    @Test
    void setCompleted_toTrue_shouldSetCompletedAt() {
        Task task = new Task(DEFAULT_TITLE, DEFAULT_DURATION);
        assertNull(task.getCompletedAt());

        // Capture time before completion
        LocalDateTime beforeCompletion = LocalDateTime.now().minusSeconds(1);

        task.setCompleted(true);

        assertTrue(task.getCompleted());
        assertNotNull(task.getCompletedAt());
        // Verify completedAt was set close to now
        assertTrue(task.getCompletedAt().isAfter(beforeCompletion));
    }

    @Test
    void setCompleted_toFalse_shouldSetCompletedAtToNull() {
        // Start as completed
        Task task = new Task(DEFAULT_TITLE, DEFAULT_DURATION);
        task.setCompleted(true);
        assertNotNull(task.getCompletedAt());

        // Mark as pending again
        task.setCompleted(false);

        assertFalse(task.getCompleted());
        assertNull(task.getCompletedAt());
    }

    @Test
    void setCompleted_trueThenTrue_shouldOnlySetCompletedAtOnce() {
        Task task = new Task(DEFAULT_TITLE, DEFAULT_DURATION);

        // 1. First completion
        task.setCompleted(true);
        LocalDateTime firstCompletionTime = task.getCompletedAt();
        assertNotNull(firstCompletionTime);

        // 2. Second set to true (should not change the time)
        try {
            // Wait a moment to ensure a time difference exists
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}

        task.setCompleted(true); // Setting to true again

        assertEquals(firstCompletionTime, task.getCompletedAt(),
                "completedAt should not change if the task is already completed.");
    }
}
