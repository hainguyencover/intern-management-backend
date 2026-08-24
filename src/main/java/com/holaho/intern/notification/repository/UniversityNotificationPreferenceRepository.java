package com.holaho.intern.notification.repository;

import com.holaho.intern.notification.entity.UniversityNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UniversityNotificationPreferenceRepository extends JpaRepository<UniversityNotificationPreference, Long> {
    Optional<UniversityNotificationPreference> findByUniversityId(Long universityId);
}
