package com.holaho.intern.repository;

import com.holaho.intern.entity.Allowance;
import com.holaho.intern.shared.enums.AllowanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface AllowanceRepository extends JpaRepository<Allowance, Long> {

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "intern", "intern.user", "paidBy" })
        java.util.Optional<Allowance> findById(Long id);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "intern", "intern.user", "paidBy" })
        Page<Allowance> findByIntern_Id(Long internId, Pageable pageable);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "intern", "intern.user", "paidBy" })
        @Query("SELECT a FROM Allowance a WHERE " +
                        "(:internId IS NULL OR a.intern.id = :internId) AND " +
                        "(:status IS NULL OR a.status = :status) AND " +
                        "(:monthFrom IS NULL OR a.allowanceMonth >= :monthFrom) AND " +
                        "(:monthTo IS NULL OR a.allowanceMonth <= :monthTo)")
        Page<Allowance> search(
                        @Param("internId") Long internId,
                        @Param("status") AllowanceStatus status,
                        @Param("monthFrom") LocalDate monthFrom,
                        @Param("monthTo") LocalDate monthTo,
                        Pageable pageable);
}

