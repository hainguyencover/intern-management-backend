package com.holaho.intern.task.repository;

import com.holaho.intern.task.entity.Task;
import com.holaho.intern.shared.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @EntityGraph(attributePaths = {"group", "createdBy", "assignee", "assignee.user", "mentor"})
    @Query("SELECT t FROM Task t")
    Page<Task> findAllWithDetails(Pageable pageable);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.group LEFT JOIN FETCH t.assignee LEFT JOIN FETCH t.assignee.user WHERE t.id = :id")
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

    @Query("SELECT t FROM Task t WHERE t.dueDate IS NOT NULL AND t.dueDate < :date AND t.status NOT IN ('APPROVED', 'DONE', 'CANCELLED')")
    List<Task> findOverdueTasks(@Param("date") LocalDateTime date);

    @Query("SELECT t FROM Task t WHERE t.dueDate BETWEEN :startDate AND :endDate")
    List<Task> findByDueDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Task> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    long countByStatus(TaskStatus status);

    long countByGroupId(Long groupId);

    long countByCreatedById(Long userId);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN GroupMember gm ON t.group.id = gm.group.id WHERE t.assignee.id = :internId OR t.assignee.user.id = :internId OR (t.assignee IS NULL AND gm.intern.id = :internId)")
    List<Task> findByAssignee_Id(@Param("internId") Long internId);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN GroupMember gm ON t.group.id = gm.group.id WHERE t.assignee.id = :internId OR t.assignee.user.id = :internId OR (t.assignee IS NULL AND gm.intern.id = :internId)")
    Page<Task> findByAssignee_Id(@Param("internId") Long internId, Pageable pageable);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN GroupMember gm ON t.group.id = gm.group.id WHERE (t.assignee.id = :internId OR t.assignee.user.id = :internId OR (t.assignee IS NULL AND gm.intern.id = :internId)) AND t.status = :status")
    Page<Task> findByAssignee_IdAndStatus(@Param("internId") Long internId, @Param("status") TaskStatus status, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.createdBy.id = :userId ORDER BY t.createdAt DESC")
    List<Task> findAssignedByMentor(@Param("userId") Long userId);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN GroupMember gm ON t.group.id = gm.group.id WHERE t.assignee.user.id = :userId OR (t.assignee IS NULL AND gm.intern.user.id = :userId) ORDER BY t.createdAt DESC")
    List<Task> findAssignedToIntern(@Param("userId") Long userId);

    @Query("SELECT t FROM Task t WHERE t.createdBy.id = :mentorId OR t.mentor.id = :mentorId")
    Page<Task> findByCreatedBy(@Param("mentorId") Long mentorId, Pageable pageable);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN GroupMember gm ON t.group.id = gm.group.id WHERE t.assignee.id = :internId OR (t.assignee IS NULL AND gm.intern.id = :internId)")
    List<Task> findByInternId(@Param("internId") Long internId);

    long countByCreatedByIdAndStatusNot(Long userId, TaskStatus status);

    List<Task> findTop5ByCreatedByIdOrderByCreatedAtDesc(Long userId);

    long countByAssignee_IdAndStatus(Long internId, TaskStatus status);

    long countByAssignee_IdAndStatusIn(Long internId, List<TaskStatus> statuses);

    @Query("SELECT COUNT(DISTINCT t) FROM Task t LEFT JOIN GroupMember gm ON t.group.id = gm.group.id " +
           "WHERE t.assignee.id = :internId OR (t.assignee IS NULL AND gm.intern.id = :internId)")
    long countByAssignee_Id(@Param("internId") Long internId);
}


