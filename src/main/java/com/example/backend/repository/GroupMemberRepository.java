//package com.example.backend.repository;
//
//import com.example.backend.entity.GroupMember;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.Optional;
//
//public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
//    Optional<GroupMember> findFirstByInternIdAndLeftAtIsNull(Long internId);
//
//    Optional<GroupMember> findByGroup_IdAndIntern_IdAndLeftAtIsNull(Long groupId, Long internId);
//}


package com.example.backend.repository;

import com.example.backend.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    // ✅ ĐÚNG với entity GroupMember có field: InternProfile intern
    Optional<GroupMember> findFirstByIntern_IdAndLeftAtIsNull(Long internId);

}
