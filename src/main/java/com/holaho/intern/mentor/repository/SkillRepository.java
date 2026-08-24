package com.holaho.intern.mentor.repository;

import com.holaho.intern.mentor.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    @Query("SELECT s FROM Skill s WHERE (s.tenantId IS NULL OR s.tenantId = :tenantId) AND s.isActive = true ORDER BY s.name ASC")
    List<Skill> findAllAvailableForTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Skill s WHERE (s.tenantId IS NULL OR s.tenantId = :tenantId) AND s.name = :name")
    Optional<Skill> findByNameAndTenantId(@Param("name") String name, @Param("tenantId") Long tenantId);
}
