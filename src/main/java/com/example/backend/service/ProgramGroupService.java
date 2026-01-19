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

        group = groupRepository.save(group);
        log.info("Created program group: {}", group.getName());

        return mapToResponse(group);
    }

    private GroupResponse mapToResponse(ProgramGroup group) {
        return new GroupResponse(group);
    }
}
