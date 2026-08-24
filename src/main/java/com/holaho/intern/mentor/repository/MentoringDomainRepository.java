package com.holaho.intern.mentor.repository;

import com.holaho.intern.mentor.entity.MentoringDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentoringDomainRepository extends JpaRepository<MentoringDomain, Long> {

    @Query("SELECT d FROM MentoringDomain d WHERE (d.tenantId IS NULL OR d.tenantId = :tenantId) AND d.isActive = true ORDER BY d.name ASC")
    List<MentoringDomain> findAllAvailableForTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT d FROM MentoringDomain d WHERE (d.tenantId IS NULL OR d.tenantId = :tenantId) AND d.name = :name")
    Optional<MentoringDomain> findByNameAndTenantId(@Param("name") String name, @Param("tenantId") Long tenantId);
}
