package com.example.backend.repository;

import com.example.backend.entity.Mentor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorRepository extends JpaRepository<Mentor, Long> {
    Optional<Mentor> findByUser_Id(Long userId);

    Optional<Mentor> findByUser_Email(String email);

    /**
     * Find all mentors in a department
     */
    List<Mentor> findByDepartment_Id(Long departmentId);

    /**
     * Count mentors in a department
     */
    long countByDepartment_Id(Long departmentId);

    boolean existsByUser_Id(Long userId);

    Page<Mentor> findAll(Pageable pageable);

    /**
     * Get mentor with user details
     */
    @Query("SELECT m FROM Mentor m JOIN FETCH m.user WHERE m.id = :id")
    Optional<Mentor> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT m FROM Mentor m WHERE m.department.id = :departmentId")
    List<Mentor> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT m FROM Mentor m JOIN FETCH m.user u WHERE m.department.id = :departmentId")
    List<Mentor> findByDepartmentIdWithUser(@Param("departmentId") Long departmentId);

    @Query("SELECT m FROM Mentor m JOIN FETCH m.user ORDER BY m.user.fullName")
    List<Mentor> findAllWithUser();

    /**
     * Get all mentors with their departments
     */
    @Query("SELECT DISTINCT m FROM Mentor m LEFT JOIN FETCH m.department LEFT JOIN FETCH m.user")
    List<Mentor> findAllWithDetails();

    /**
     * Find mentors by title
     */
    /**
     * Find mentors excluding those who have INTERN role
     */
    @Query("SELECT m FROM Mentor m JOIN m.user u WHERE NOT EXISTS (SELECT r FROM u.roles r WHERE r.code = 'INTERN' OR r.code = 'ROLE_INTERN')")
    Page<Mentor> findMentorsExcludingInterns(Pageable pageable);

    @Query("SELECT m FROM Mentor m JOIN FETCH m.user u WHERE NOT EXISTS (SELECT r FROM u.roles r WHERE r.code = 'INTERN' OR r.code = 'ROLE_INTERN') ORDER BY m.user.fullName")
    List<Mentor> findAllWithUserExcludingInterns();
}
