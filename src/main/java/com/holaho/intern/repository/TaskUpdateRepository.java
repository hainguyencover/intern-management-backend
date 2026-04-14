package com.holaho.intern.repository;

import com.holaho.intern.entity.TaskUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskUpdateRepository extends JpaRepository<TaskUpdate, Long> {

        List<TaskUpdate> findByTaskId(Long taskId);

        List<TaskUpdate> findByTaskIdOrderByCreatedAtDesc(Long taskId);

        @Query("SELECT tu FROM TaskUpdate tu " +
                        "JOIN FETCH tu.task " +
                        "JOIN FETCH tu.intern " +
                        "WHERE tu.task.id = :taskId " +
                        "ORDER BY tu.createdAt DESC")
        List<TaskUpdate> findByTaskIdWithDetails(@Param("taskId") Long taskId);

        List<TaskUpdate> findByInternId(Long internId);

        @Query("SELECT tu FROM TaskUpdate tu " +
                        "WHERE tu.intern.id = :internId " +
                        "ORDER BY tu.createdAt DESC")
        List<TaskUpdate> findByInternIdOrderByCreatedAtDesc(@Param("internId") Long internId);

        Optional<TaskUpdate> findFirstByTaskIdOrderByCreatedAtDesc(Long taskId);

        @Query("SELECT AVG(tu.progressPercent) FROM TaskUpdate tu WHERE tu.task.id = :taskId")
        Double findAverageProgressByTaskId(@Param("taskId") Long taskId);

        long countByTaskId(Long taskId);

        /**
         * Count updates for a task
         */
        int countByTask_Id(Long taskId);

        /**
         * Count updates by intern
         */
        long countByIntern_Id(Long internId);

        /**
         * Get latest update for a task
         */
        @Query("SELECT tu FROM TaskUpdate tu WHERE tu.task.id = :taskId " +
                        "ORDER BY tu.createdAt DESC LIMIT 1")
        TaskUpdate findLatestByTaskId(@Param("taskId") Long taskId);

        /**
         * Get latest progress percent for a task
         */
        @Query("SELECT tu.progressPercent FROM TaskUpdate tu WHERE tu.task.id = :taskId " +
                        "AND tu.progressPercent IS NOT NULL ORDER BY tu.createdAt DESC LIMIT 1")
        Integer findLatestProgressByTaskId(@Param("taskId") Long taskId);

        List<TaskUpdate> findByTask_Id(Long taskId);

        org.springframework.data.domain.Page<TaskUpdate> findByTaskId(Long taskId,
                        org.springframework.data.domain.Pageable pageable);

        List<TaskUpdate> findByIntern_Id(Long internId);
}

