package com.holaho.intern.mentor.repository;

import com.holaho.intern.mentor.entity.MentorSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorSkillRepository extends JpaRepository<MentorSkill, Long> {

    List<MentorSkill> findByMentorId(Long mentorId);

    Optional<MentorSkill> findByMentorIdAndSkillId(Long mentorId, Long skillId);

    boolean existsByMentorIdAndSkillId(Long mentorId, Long skillId);

    void deleteByMentorIdAndSkillId(Long mentorId, Long skillId);
}
