package com.holaho.intern.task.repository;

import com.holaho.intern.entity.GroupMember;


import com.holaho.intern.task.entity.Task;
import com.holaho.intern.shared.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<Task> {

        @Query("SELECT t FROM Task t JOIN FETCH t.group WHERE t.id = :id")
        Optional<Task> findByIdWithGroup(@Param("id") Long id);

        List<Task> findByGroupId(Long groupId);

        Page<Task> findByGroupId(Long groupId, Pageable pageable);

        List<Task> findByStatus(TaskStatus status);

        Page<Task> findByStatus(TaskStatus status, Pageable pageable);

        Page<Task> findByGroupIdAndStatus(Long groupId, TaskStatus status, Pageable pageable);

        @Query("SELECT t FROM Task t WHERE t.createdBy.id = :userId")
        List<Task> findByCreatedById(@Param("userId") Long userId);

        @Query("SELECT t FROM Task t WHERE t.createdBy.id = :userId AND t.status = :status")
        Page<Task> findByCreatedByIdAndStatus(
                        @Param("userId") Long userId,
                        @Param("status") TaskStatus status,
                        Pageable pageable);

        @Query("SELECT t FROM Task t " +
                        "WHERE t.dueDate IS NOT NULL AND t.dueDate < :date AND t.status NOT IN ('DONE', 'CANCELLED')")
        List<Task> findOverdueTasks(@Param("date") LocalDateTime date);

        @Query("SELECT t FROM Task t " +
                        "WHERE t.dueDate BETWEEN :startDate AND :endDate")
        List<Task> findByDueDateBetween(
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT t FROM Task t " +
                        "WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<Task> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        long countByStatus(TaskStatus status);

        long countByGroupId(Long groupId);

        long countByCreatedById(Long userId);

        Page<Task> findByAssignee_Id(Long internId, Pageable pageable);

        Page<Task> findByAssignee_IdAndStatus(Long internId, TaskStatus status, Pageable pageable);

        @Query("select t from Task t where t.createdBy.id = :userId order by t.createdAt desc")
        List<Task> findAssignedByMentor(@Param("userId") Long userId);

        @Query("""
                            select distinct t from Task t
                            join t.group g
                            join GroupMember gm on gm.group = g
                            where gm.intern.user.id = :userId
                            order by t.createdAt desc
                        """)
        List<Task> findAssignedToIntern(@Param("userId") Long userId);

        @Query("SELECT t FROM Task t WHERE t.createdBy.id = :mentorId")
        Page<Task> findByCreatedBy(@Param("mentorId") Long mentorId, Pageable pageable);

        @Query("SELECT t FROM Task t JOIN t.group g JOIN GroupMember gm ON gm.group.id = g.id WHERE gm.intern.id = :internId")
        List<Task> findByInternId(@Param("internId") Long internId);

        long countByCreatedByIdAndStatusNot(Long userId, TaskStatus status);

        List<Task> findTop5ByCreatedByIdOrderByCreatedAtDesc(Long userId);
}

