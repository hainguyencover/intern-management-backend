package com.example.backend.service;

import com.example.backend.dto.response.GroupResponse;
import com.example.backend.entity.*;
import com.example.backend.enums.GroupStatus;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramGroupService {

    private final ProgramGroupRepository groupRepository;
    private final ProgramRepository programRepository;
    private final GroupMemberRepository memberRepository;
    private final InternProfileRepository internProfileRepository;

    @Transactional
    public GroupResponse createGroup(ProgramGroup request) {
        Program program = programRepository.findById(request.getProgram().getId())
                .orElseThrow(() -> new RuntimeException("Program not found: " + request.getProgram().getId()));

        ProgramGroup group = new ProgramGroup();
        group.setProgram(program);
        group.setName(request.getName());
        group.setDepartmentId(request.getDepartmentId());
        group.setMentorId(request.getMentorId());
        group.setStatus(GroupStatus.ACTIVE);

        validateMentorSchedule(request.getMentorId(), group.getWorkDays(), group.getWorkStartTime(),
                group.getWorkEndTime(), program, null);

        group = groupRepository.save(group);
        log.info("Created program group: {}", group.getName());

        return mapToResponse(group);
    }

    @Transactional
    public void assignIntern(Long groupId, Long internId) {
        ProgramGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new RuntimeException("Intern not found: " + internId));

        if (memberRepository.existsByGroupIdAndInternId(groupId, internId)) {
            throw new RuntimeException("Intern already assigned to this group");
        }

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setIntern(intern);
        member.setJoinedAt(LocalDateTime.now());

        memberRepository.save(member);

        // Auto-update intern's start/end date from Program if not set or if needed
        Program program = group.getProgram();
        if (program != null) {
            boolean updated = false;
            // Always overwrite or only if null? Let's overwrite to ensure sync with Program
            if (program.getStartDate() != null) {
                intern.setStartDate(program.getStartDate());
                updated = true;
            }
            if (program.getEndDate() != null) {
                intern.setEndDate(program.getEndDate());
                updated = true;
            }
            if (updated) {
                internProfileRepository.save(intern);
            }
        }

        log.info("Assigned intern {} to group {}", internId, groupId);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getGroupsByProgramId(Long programId) {
        return groupRepository.findByProgramId(programId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<GroupResponse> search(Long programId, String statusStr, String keyword, Pageable pageable) {
        Specification<ProgramGroup> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (programId != null) {
                predicates.add(cb.equal(root.get("program").get("id"), programId));
            }

            if (StringUtils.hasText(keyword)) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), likePattern));
            }

            if (StringUtils.hasText(statusStr)) {
                try {
                    GroupStatus status = GroupStatus.valueOf(statusStr.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore or maybe return empty?
                    // For now, let's ignore checking status if it's invalid (like "SUBMITTED")
                    log.warn("Invalid GroupStatus: {}", statusStr);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return groupRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(Long id) {
        ProgramGroup group = groupRepository.findByIdWithProgram(id)
                .orElseThrow(() -> new RuntimeException("Group not found: " + id));
        return mapToResponse(group);
    }

    @Transactional(readOnly = true)
    public List<com.example.backend.dto.response.GroupMemberResponse> getMembers(Long groupId) {
        return memberRepository.findByGroupId(groupId).stream()
                .map(member -> com.example.backend.dto.response.GroupMemberResponse.builder()
                        .id(member.getId())
                        .groupId(member.getGroup().getId())
                        .internId(member.getIntern().getId())
                        .internName(member.getIntern().getUser().getFullName())
                        .internEmail(member.getIntern().getUser().getEmail())
                        .studentCode(member.getIntern().getStudentCode())
                        .joinedAt(member.getJoinedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupResponse update(Long id, com.example.backend.dto.request.UpdateGroupRequest request) {
        ProgramGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found: " + id));

        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getMentorId() != null) {
            group.setMentorId(request.getMentorId());
        }
        if (request.getStatus() != null) {
            group.setStatus(request.getStatus());
        }
        if (request.getWorkStartTime() != null) {
            group.setWorkStartTime(request.getWorkStartTime());
        }
        if (request.getWorkEndTime() != null) {
            group.setWorkEndTime(request.getWorkEndTime());
        }
        if (request.getWorkDays() != null) {
            group.setWorkDays(request.getWorkDays());
        }

        // Validate schedule if mentor is set (assuming mentor is mandatory for active
        // groups)
        if (group.getStatus() == GroupStatus.ACTIVE && group.getMentorId() != null) {
            validateMentorSchedule(group.getMentorId(), group.getWorkDays(), group.getWorkStartTime(),
                    group.getWorkEndTime(), group.getProgram(), id);
        }

        group = groupRepository.save(group);
        log.info("Updated program group: {}", id);
        return mapToResponse(group);
    }

    @Transactional
    public void delete(Long id) {
        if (!groupRepository.existsById(id)) {
            throw new RuntimeException("Group not found: " + id);
        }
        groupRepository.deleteById(id);
        log.info("Deleted program group: {}", id);
    }

    @Transactional
    public void assignInterns(Long groupId, List<Long> internIds) {
        for (Long internId : internIds) {
            try {
                assignIntern(groupId, internId);
            } catch (Exception e) {
                log.warn("Failed to assign intern {} to group {}: {}", internId, groupId, e.getMessage());
            }
        }
    }

    @Transactional
    public void removeIntern(Long groupId, Long internId) {
        GroupMember member = memberRepository.findByGroupIdAndInternId(groupId, internId)
                .orElseThrow(() -> new RuntimeException("Member not found in group"));

        memberRepository.delete(member);
        log.info("Removed intern {} from group {}", internId, groupId);
    }

    // Alias for controller calling getByProgramId
    @Transactional(readOnly = true)
    public List<GroupResponse> getByProgramId(Long programId) {
        return getGroupsByProgramId(programId);
    }

    // Alias for controller calling getById
    @Transactional(readOnly = true)
    public GroupResponse getById(Long id) {
        return getGroupById(id);
    }

    // Alias for controller calling create
    @Transactional
    public GroupResponse create(com.example.backend.dto.request.GroupRequest request) {
        Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new RuntimeException("Program not found: " + request.getProgramId()));

        ProgramGroup group = new ProgramGroup();
        group.setProgram(program);
        group.setName(request.getName());
        group.setDepartmentId(request.getDepartmentId());
        group.setMentorId(request.getMentorId());
        group.setStatus(GroupStatus.ACTIVE);
        group.setWorkStartTime(request.getWorkStartTime());
        group.setWorkEndTime(request.getWorkEndTime());
        group.setWorkDays(request.getWorkDays());

        validateMentorSchedule(request.getMentorId(), group.getWorkDays(), group.getWorkStartTime(),
                group.getWorkEndTime(), program, null);

        group = groupRepository.save(group);
        log.info("Created program group: {}", group.getName());

        return mapToResponse(group);
    }

    private GroupResponse mapToResponse(ProgramGroup group) {
        return new GroupResponse(group);
    }

    private void validateMentorSchedule(Long mentorId, String workDaysStr, java.time.LocalTime start,
            java.time.LocalTime end, Program currentProgram, Long excludeGroupId) {
        if (mentorId == null || !StringUtils.hasText(workDaysStr) || start == null || end == null) {
            return;
        }

        List<ProgramGroup> activeGroups = groupRepository.findByMentorIdAndStatus(mentorId, GroupStatus.ACTIVE);

        for (ProgramGroup g : activeGroups) {
            if (excludeGroupId != null && g.getId().equals(excludeGroupId)) {
                continue;
            }

            // Must overlap in DATE (Program duration) first
            if (currentProgram != null && g.getProgram() != null && !hasDateOverlap(
                    currentProgram.getStartDate(), currentProgram.getEndDate(),
                    g.getProgram().getStartDate(), g.getProgram().getEndDate())) {
                continue; // Different periods (e.g. Jan vs Mar) -> No conflict
            }

            if (g.getWorkDays() == null || g.getWorkStartTime() == null || g.getWorkEndTime() == null) {
                continue;
            }

            if (hasDayOverlap(workDaysStr, g.getWorkDays())
                    && hasTimeOverlap(start, end, g.getWorkStartTime(), g.getWorkEndTime())) {
                throw new com.example.backend.exception.BadRequestException(
                        "Mentor đã có lịch dạy tại nhóm: " + g.getName() + " (Chương trình: " + g.getProgram().getName()
                                + ")");
            }
        }
    }

    private boolean hasDayOverlap(String days1, String days2) {
        String[] d1 = days1.split(",");
        String[] d2 = days2.split(",");
        for (String s1 : d1) {
            for (String s2 : d2) {
                if (s1.trim().equalsIgnoreCase(s2.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasTimeOverlap(java.time.LocalTime start1, java.time.LocalTime end1, java.time.LocalTime start2,
            java.time.LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private boolean hasDateOverlap(java.time.LocalDate start1, java.time.LocalDate end1, java.time.LocalDate start2,
            java.time.LocalDate end2) {
        // If undefined dates, assume infinite -> overlap
        if (start1 == null && end1 == null)
            return true;
        if (start2 == null && end2 == null)
            return true;

        java.time.LocalDate s1 = start1 != null ? start1 : java.time.LocalDate.MIN;
        java.time.LocalDate e1 = end1 != null ? end1 : java.time.LocalDate.MAX;
        java.time.LocalDate s2 = start2 != null ? start2 : java.time.LocalDate.MIN;
        java.time.LocalDate e2 = end2 != null ? end2 : java.time.LocalDate.MAX;

        return s1.isBefore(e2) && s2.isBefore(e1);
    }

}
