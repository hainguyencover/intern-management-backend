package com.holaho.intern.repository;

import com.holaho.intern.entity.GroupMember;


import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.shared.enums.GroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramGroupRepository extends JpaRepository<ProgramGroup, Long>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<ProgramGroup> {
        @Query("SELECT g FROM ProgramGroup g JOIN FETCH g.program WHERE g.id = :id")
        Optional<ProgramGroup> findByIdWithProgram(@Param("id") Long id);

        /**
         * Find groups by program ID
         */
        List<ProgramGroup> findByProgramId(Long programId);

        @Query("SELECT g FROM ProgramGroup g WHERE g.mentorId = :mentorId")
        List<ProgramGroup> findByMentorId(@Param("mentorId") Long mentorId);

        /**
         * Find groups by department ID
         */
        List<ProgramGroup> findByDepartmentId(Long departmentId);

        /**
         * Find groups by status
         */
        List<ProgramGroup> findByStatus(GroupStatus status);

        @Query("SELECT g FROM ProgramGroup g JOIN FETCH g.program " +
                        "WHERE g.mentorId = :mentorId AND g.status = :status")
        List<ProgramGroup> findByMentorIdAndStatus(
                        @Param("mentorId") Long mentorId,
                        @Param("status") GroupStatus status);

        long countByProgramId(Long programId);

        long countByMentorId(Long mentorId);

        long countByStatus(GroupStatus status);

        /**
         * Count members in a group
         */
        @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId")
        Integer countMembersByGroupId(@Param("groupId") Long groupId);

        /**
         * Find groups with member count
         */
        @Query("SELECT g FROM ProgramGroup g WHERE g.program.id = :programId")
        List<ProgramGroup> findByProgramIdWithMembers(@Param("programId") Long programId);

        List<ProgramGroup> findByProgramIdAndStatus(Long programId, GroupStatus status);
}

