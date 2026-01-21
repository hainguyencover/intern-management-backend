package com.example.backend.repository;

import com.example.backend.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    @Query("select gm.intern.id from GroupMember gm where gm.group.id = :groupId")
    List<Long> findInternIdsByGroupId(@Param("groupId") Long groupId);

    Optional<GroupMember> findFirstByInternIdAndLeftAtIsNull(Long internId);

    List<GroupMember> findByGroup_Id(Long groupId);

    Optional<GroupMember> findByGroupIdAndInternIdAndLeftAtIsNull(Long groupId, Long internId);

    List<GroupMember> findAllByGroup_IdAndLeftAtIsNull(Long groupId);
}
