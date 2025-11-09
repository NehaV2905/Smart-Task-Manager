package com.taskmanager.repository;

import com.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Find all tasks ordered by creation date (newest first)
    List<Task> findAllByOrderByCreatedAtDesc();

    // Find completed tasks
    List<Task> findByCompletedTrue();

    // Find incomplete tasks
    List<Task> findByCompletedFalse();

    // Count completed tasks
    Long countByCompletedTrue();

    // Count total tasks
    Long countBy();
}