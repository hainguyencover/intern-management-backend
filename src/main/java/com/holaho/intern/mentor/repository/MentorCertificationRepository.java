package com.holaho.intern.mentor.repository;

import com.holaho.intern.mentor.entity.MentorCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorCertificationRepository extends JpaRepository<MentorCertification, Long> {

    List<MentorCertification> findByMentorIdOrderByIssuedDateDesc(Long mentorId);
}
