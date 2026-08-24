package com.holaho.intern.mentor.repository;

import com.holaho.intern.mentor.entity.MentorExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorExperienceRepository extends JpaRepository<MentorExperience, Long> {

    List<MentorExperience> findByMentorIdOrderByStartDateDesc(Long mentorId);
}
