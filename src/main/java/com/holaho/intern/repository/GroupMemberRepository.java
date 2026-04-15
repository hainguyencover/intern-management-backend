package com.holaho.intern.repository;

import com.holaho.intern.intern.entity.InternProfile;


import com.holaho.intern.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

        // Tìm members trong group
        List<GroupMember> findByGroupId(Long groupId);

        @Query("SELECT gm FROM GroupMember gm " +
                        "JOIN FETCH gm.intern i " +
                        "JOIN FETCH i.user " +
                        "WHERE gm.group.id = :groupId")
        List<GroupMember> findByGroupIdWithIntern(@Param("groupId") Long groupId);

        // Tìm groups của một intern
        List<GroupMember> findByInternId(Long internId);

        /**
         * Find a specific membership
         */
        Optional<GroupMember> findByGroup_IdAndIntern_Id(Long groupId, Long internId);

        /**
         * Count members in a group
         */
        long countByGroup_Id(Long groupId);

        /**
         * Count groups an intern belongs to
         */
        long countByIntern_Id(Long internId);

        // ✅ ĐÚNG với entity GroupMember có field: InternProfile intern
        Optional<GroupMember> findFirstByIntern_IdAndLeftAtIsNull(Long internId);

        @Query("""
                            select count(gm) > 0
                            from GroupMember gm
                            where gm.group.id = :groupId
                              and gm.intern.id = :internId
                              and gm.leftAt is null
                        """)
        boolean existsActiveInGroup(@Param("groupId") Long groupId, @Param("internId") Long internId);

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

        @Query("SELECT COUNT(gm) FROM GroupMember gm " +
                        "WHERE gm.group.id = :groupId AND gm.leftAt IS NULL")
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

        boolean existsByGroup_IdAndIntern_IdAndLeftAtIsNull(Long groupId, Long internId);

        // Check intern đã trong group chưa (và chưa left)
        @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId " +
                        "AND gm.intern.id = :internId AND gm.leftAt IS NULL")
        Optional<GroupMember> findActiveByGroupAndIntern(
                        @Param("groupId") Long groupId,
                        @Param("internId") Long internId);

        // Lấy danh sách intern active trong group
        @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.leftAt IS NULL")
        List<GroupMember> findActiveByGroupId(@Param("groupId") Long groupId);

        boolean existsByGroup_IdAndIntern_Id(Long groupId, Long internId);

        /**
         * Find all active memberships for an intern
         */
        @Query("SELECT gm FROM GroupMember gm WHERE gm.intern.id = :internId AND gm.leftAt IS NULL")
        List<GroupMember> findActiveByInternId(@Param("internId") Long internId);

        Optional<GroupMember> findByGroupIdAndInternId(Long groupId, Long internId);

        boolean existsByGroupIdAndInternId(Long groupId, Long internId);

        long countByGroupId(Long groupId);

        @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.leftAt IS NULL")
        List<GroupMember> findActiveMembers(@Param("groupId") Long groupId);

        long countByGroup_ProgramId(Long programId);
}
