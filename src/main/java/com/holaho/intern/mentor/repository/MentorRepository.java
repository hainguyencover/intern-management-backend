package com.holaho.intern.mentor.repository;

import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.shared.enums.MentorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface MentorRepository extends JpaRepository<Mentor, Long> {

    Optional<Mentor> findByTenantIdAndId(Long tenantId, Long id);

    Optional<Mentor> findByUser_Id(Long userId);

    Optional<Mentor> findByTenantIdAndUser_Id(Long tenantId, Long userId);

    Optional<Mentor> findByUser_Email(String email);

    boolean existsByTenantIdAndEmployeeCode(Long tenantId, String employeeCode);

    Optional<Mentor> findByTenantIdAndEmployeeCode(Long tenantId, String employeeCode);

    List<Mentor> findByTenantIdAndDepartment_Id(Long tenantId, Long departmentId);

    long countByTenantIdAndDepartment_Id(Long tenantId, Long departmentId);

    boolean existsByUser_Id(Long userId);

    Page<Mentor> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT m FROM Mentor m LEFT JOIN FETCH m.user LEFT JOIN FETCH m.department WHERE m.tenantId = :tenantId AND m.id = :id")
    Optional<Mentor> findByTenantIdAndIdWithDetails(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @Query("SELECT m FROM Mentor m JOIN FETCH m.user WHERE m.id = :id")
    Optional<Mentor> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT m FROM Mentor m WHERE m.department.id = :departmentId")
    List<Mentor> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT m FROM Mentor m JOIN FETCH m.user u WHERE m.department.id = :departmentId")
    List<Mentor> findByDepartmentIdWithUser(@Param("departmentId") Long departmentId);

    @Query("SELECT m FROM Mentor m JOIN FETCH m.user ORDER BY m.user.fullName")
    List<Mentor> findAllWithUser();

    @Query("SELECT DISTINCT m FROM Mentor m LEFT JOIN FETCH m.department LEFT JOIN FETCH m.user WHERE m.tenantId = :tenantId")
    List<Mentor> findAllByTenantIdWithDetails(@Param("tenantId") Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Mentor m WHERE m.tenantId = :tenantId AND m.id = :id")
    Optional<Mentor> findByTenantIdAndIdWithLock(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @Query("""
        SELECT m FROM Mentor m
        LEFT JOIN m.user u
        LEFT JOIN m.department d
        WHERE m.tenantId = :tenantId
          AND (:status IS NULL OR m.status = :status)
          AND (:departmentId IS NULL OR d.id = :departmentId)
          AND (
               :search IS NULL OR :search = '' OR
               LOWER(m.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(m.employeeCode) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(m.specialization) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<Mentor> searchMentors(
            @Param("tenantId") Long tenantId,
            @Param("search") String search,
            @Param("status") MentorStatus status,
            @Param("departmentId") Long departmentId,
            Pageable pageable
    );
}
