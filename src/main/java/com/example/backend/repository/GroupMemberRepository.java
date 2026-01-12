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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    // ✅ ĐÚNG với entity GroupMember có field: InternProfile intern
    Optional<GroupMember> findFirstByIntern_IdAndLeftAtIsNull(Long internId);

    @Query("""
        select count(gm) > 0
        from GroupMember gm
        where gm.group.id = :groupId
          and gm.intern.id = :internId
          and gm.leftAt is null
    """)
    boolean existsActiveInGroup(@Param("groupId") Long groupId,
                                @Param("internId") Long internId);

    @Query("""
        select count(gm) > 0
        from GroupMember gm
        join gm.group g
        where gm.intern.id = :internId
          and g.program.id = :programId
          and gm.leftAt is null
    """)
    boolean existsActiveInProgram(@Param("internId") Long internId,
                                  @Param("programId") Long programId);

    @Query("""
        select gm
        from GroupMember gm
        where gm.group.id = :groupId
          and gm.leftAt is null
        order by gm.joinedAt desc
    """)
    List<GroupMember> findActiveMembersByGroupId(@Param("groupId") Long groupId);

    @Query("""
        select count(gm)
        from GroupMember gm
        where gm.group.id = :groupId
          and gm.leftAt is null
    """)
    long countActiveByGroupId(@Param("groupId") Long groupId);

    @Query("""
        select gm
        from GroupMember gm
        where gm.group.id = :groupId
          and gm.intern.id = :internId
          and gm.leftAt is null
    """)
    Optional<GroupMember> findActiveMembership(@Param("groupId") Long groupId,
                                               @Param("internId") Long internId);
}
