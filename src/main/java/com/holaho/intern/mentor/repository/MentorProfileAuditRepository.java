package com.holaho.intern.mentor.repository;

import com.holaho.intern.mentor.entity.MentorProfileAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorProfileAuditRepository extends JpaRepository<MentorProfileAudit, Long> {

    List<MentorProfileAudit> findByMentorIdOrderByCreatedAtDesc(Long mentorId);
}
