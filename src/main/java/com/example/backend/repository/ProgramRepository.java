package com.example.backend.repository;

import com.example.backend.dto.ProgramCompletionStatDto;
import com.example.backend.entity.Program;
import com.example.backend.enums.ProgramStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        @Query("SELECT p FROM Program p WHERE p.department.id = :departmentId")
        List<Program> findByDepartmentId(@Param("departmentId") Long departmentId);

        @Query("SELECT p FROM Program p " +
                        "WHERE p.department.id = :departmentId AND p.status = :status")
        List<Program> findByDepartmentIdAndStatus(
                        @Param("departmentId") Long departmentId,
                        @Param("status") ProgramStatus status);

        // Search với filter nâng cao
        @Query("SELECT p FROM Program p " +
                        "WHERE (:departmentId IS NULL OR p.department.id = :departmentId) " +
                        "AND (:status IS NULL OR p.status = :status) " +
                        "AND (:keyword IS NULL OR :keyword = '' OR " +
                        "     LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "     LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "ORDER BY p.createdAt DESC")
        Page<Program> search(
                        @Param("departmentId") Long departmentId,
                        @Param("status") ProgramStatus status,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("SELECT p FROM Program p " +
                        "WHERE p.status = 'ACTIVE' " +
                        "AND p.startDate <= CURRENT_DATE " +
                        "AND p.endDate >= CURRENT_DATE")
        List<Program> findCurrentlyActive();

        @Query("SELECT p FROM Program p " +
                        "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<Program> searchByName(@Param("keyword") String keyword, Pageable pageable);

        long countByStatus(ProgramStatus status);

        long countByDepartmentId(Long departmentId);

        @Query("SELECT p FROM Program p " +
                        "WHERE p.startDate <= :date AND p.endDate >= :date")
        List<Program> findActiveOnDate(@Param("date") LocalDate date);

        // Đếm program theo department và status
        long countByDepartment_IdAndStatus(Long departmentId, ProgramStatus status);

        Page<Program> findAllByStatus(ProgramStatus status, Pageable pageable);

        Page<Program> findAllByDepartment_IdAndStatus(Long departmentId, ProgramStatus status, Pageable pageable);

        boolean existsByDepartment_Id(Long departmentId);

        @Query("""
                        select new com.example.backend.dto.ProgramCompletionStatDto(
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
