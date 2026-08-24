package com.holaho.intern.repository;

import com.holaho.intern.entity.GroupMember;
import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.entity.Evaluation;


import com.holaho.intern.shared.dto.ProgramCompletionStatDto;
import com.holaho.intern.entity.Program;
import com.holaho.intern.shared.enums.ProgramStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {
        @Query("SELECT p FROM Program p JOIN FETCH p.department WHERE p.id = :id")
        Optional<Program> findByIdWithDepartment(@Param("id") Long id);

        // Tìm program theo status
        List<Program> findByStatus(ProgramStatus status);

        Page<Program> findByStatus(ProgramStatus status, Pageable pageable);

        @EntityGraph(attributePaths = {"department"})
        @Query("SELECT p FROM Program p WHERE p.department.id = :departmentId")
        List<Program> findByDepartmentId(@Param("departmentId") Long departmentId);

        @EntityGraph(attributePaths = {"department"})
        @Query("SELECT p FROM Program p " +
                        "WHERE p.department.id = :departmentId AND p.status = :status")
        List<Program> findByDepartmentIdAndStatus(
                        @Param("departmentId") Long departmentId,
                        @Param("status") ProgramStatus status);

        boolean existsByCode(String code);

        boolean existsByCodeAndIdNot(String code, Long id);

        // Search với filter nâng cao
        @EntityGraph(attributePaths = {"department"})
        @Query(value = "SELECT p FROM Program p " +
                        "WHERE (:departmentId IS NULL OR p.department.id = :departmentId) " +
                        "AND (:status IS NULL OR p.status = :status) " +
                        "AND (:keyword IS NULL OR " +
                        "     LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "     LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "     LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))",
               countQuery = "SELECT COUNT(p) FROM Program p " +
                        "WHERE (:departmentId IS NULL OR p.department.id = :departmentId) " +
                        "AND (:status IS NULL OR p.status = :status) " +
                        "AND (:keyword IS NULL OR " +
                        "     LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "     LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "     LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<Program> search(
                        @Param("departmentId") Long departmentId,
                        @Param("status") ProgramStatus status,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @EntityGraph(attributePaths = {"department"})
        @Query("SELECT p FROM Program p " +
                        "WHERE p.status = 'ACTIVE' " +
                        "AND p.startDate <= CURRENT_DATE " +
                        "AND p.endDate >= CURRENT_DATE")
        List<Program> findCurrentlyActive();

        @EntityGraph(attributePaths = {"department"})
        @Query("SELECT p FROM Program p " +
                        "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<Program> searchByName(@Param("keyword") String keyword, Pageable pageable);

        long countByStatus(ProgramStatus status);

        long countByDepartmentId(Long departmentId);

        @EntityGraph(attributePaths = {"department"})
        @Query("SELECT p FROM Program p " +
                        "WHERE p.startDate <= :date AND p.endDate >= :date")
        List<Program> findActiveOnDate(@Param("date") LocalDate date);

        // Đếm program theo department và status
        long countByDepartment_IdAndStatus(Long departmentId, ProgramStatus status);

        @EntityGraph(attributePaths = {"department"})
        Page<Program> findAllByStatus(ProgramStatus status, Pageable pageable);

        @EntityGraph(attributePaths = {"department"})
        Page<Program> findAllByDepartment_IdAndStatus(Long departmentId, ProgramStatus status, Pageable pageable);

        boolean existsByDepartment_Id(Long departmentId);

        @Query("""
                        select new com.holaho.intern.shared.dto.ProgramCompletionStatDto(
                                p.id,
                                p.name,
                                count(distinct gm.intern.id),
                                count(distinct e.intern.id)
                            )
                            from Program p
                            join ProgramGroup g on g.program.id = p.id
                            join GroupMember gm on gm.group.id = g.id
                            left join Evaluation e
                                on e.intern.id = gm.intern.id
                               and e.period = :period
                            where (:departmentId is null or p.department.id = :departmentId)
                            group by p.id, p.name
                            order by p.id desc
                        """)
        List<ProgramCompletionStatDto> rawCompletionByProgram(
                        @Param("period") String period,
                        @Param("departmentId") Long departmentId);

        @Query("SELECT p FROM Program p WHERE p.startDate <= :date AND p.endDate >= :date")
        List<Program> findActivePrograms(@Param("date") LocalDate date);

        List<Program> findByStatusAndEndDateBefore(ProgramStatus status, LocalDate date);
}
