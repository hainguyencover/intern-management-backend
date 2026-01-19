package com.example.backend.repository;

import com.example.backend.entity.InternshipContract;
import com.example.backend.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternshipContractRepository extends JpaRepository<InternshipContract, Long> {

    @Query("SELECT c FROM InternshipContract c " +
            "JOIN FETCH c.application a " +
            "JOIN FETCH a.intern i " +
            "JOIN FETCH i.user " +
            "WHERE c.id = :id")
    Optional<InternshipContract> findByIdWithDetails(@Param("id") Long id);

    Optional<InternshipContract> findByApplication_Id(Long applicationId);

    Optional<InternshipContract> findByApplicationId(Long applicationId);

    @Query("SELECT c FROM InternshipContract c WHERE c.application.intern.id = :internId ORDER BY c.createdAt DESC")
    List<InternshipContract> findByInternId(@Param("internId") Long internId);

    boolean existsByApplicationId(Long applicationId);

    List<InternshipContract> findByStatus(ContractStatus status);

    @Query("SELECT c FROM InternshipContract c " +
            "JOIN c.application a " +
            "WHERE a.intern.id = :internId AND c.status = :status")
    List<InternshipContract> findByInternIdAndStatus(
            @Param("internId") Long internId,
            @Param("status") ContractStatus status);

    long countByStatus(ContractStatus status);

    @Query("SELECT c FROM InternshipContract c WHERE c.application.intern.user.id = :userId ORDER BY c.createdAt DESC")
    List<InternshipContract> findByUserId(@Param("userId") Long userId);
}
