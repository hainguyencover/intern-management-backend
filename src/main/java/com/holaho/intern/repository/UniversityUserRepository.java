package com.holaho.intern.repository;

import com.holaho.intern.entity.UniversityUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UniversityUserRepository extends JpaRepository<UniversityUser, Long> {
    Optional<UniversityUser> findByUserId(Long userId);

    java.util.List<UniversityUser> findByUniversityId(Long universityId);
}
