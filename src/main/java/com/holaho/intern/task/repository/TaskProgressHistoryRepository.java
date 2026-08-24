package com.holaho.intern.task.repository;

import com.holaho.intern.task.entity.TaskProgressHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskProgressHistoryRepository extends JpaRepository<TaskProgressHistory, Long> {

    @Query("SELECT h FROM TaskProgressHistory h JOIN FETCH h.changedBy WHERE h.task.id = :taskId ORDER BY h.changedAt DESC")
    List<TaskProgressHistory> findByTaskIdOrderByChangedAtDesc(@Param("taskId") Long taskId);

    @Query("SELECT h FROM TaskProgressHistory h JOIN FETCH h.changedBy WHERE h.task.id = :taskId ORDER BY h.changedAt DESC")
    Page<TaskProgressHistory> findByTaskIdOrderByChangedAtDesc(@Param("taskId") Long taskId, Pageable pageable);
}
