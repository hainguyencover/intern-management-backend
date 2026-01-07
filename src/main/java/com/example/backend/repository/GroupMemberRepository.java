package com.example.backend.repository;

import com.example.backend.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findFirstByInternIdAndLeftAtIsNull(Long internId);

    Optional<GroupMember> findByGroupIdAndInternIdAndLeftAtIsNull(Long groupId, Long internId);
}
