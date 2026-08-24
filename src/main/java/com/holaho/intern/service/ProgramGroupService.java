package com.holaho.intern.service;

import com.holaho.intern.entity.GroupMember;
import com.holaho.intern.entity.Program;
import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.service.validator.MentorScheduleValidator;
import com.holaho.intern.shared.dto.request.GroupRequest;
import com.holaho.intern.shared.dto.request.UpdateGroupRequest;
import com.holaho.intern.shared.dto.response.GroupMemberResponse;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;


import com.holaho.intern.shared.dto.response.GroupResponse;
import com.holaho.intern.shared.enums.GroupStatus;
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
import java.util.Optional;
import java.util.stream.Collectors;

import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.mentor.service.MentorAssignmentService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramGroupService {

    private final ProgramGroupRepository groupRepository;
    private final ProgramRepository programRepository;
    private final GroupMemberRepository memberRepository;
    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;
    private final MentorAssignmentService mentorAssignmentService;
    private final MentorScheduleValidator mentorScheduleValidator;

    @Transactional
    public GroupResponse createGroup(ProgramGroup request) {
        Program program = programRepository.findById(request.getProgram().getId())
                .orElseThrow(() -> new NotFoundException("Program", request.getProgram().getId()));

        ProgramGroup group = new ProgramGroup();
        group.setProgram(program);
        group.setName(request.getName());
        group.setDepartmentId(request.getDepartmentId());
        group.setMentorId(request.getMentorId());
        group.setStatus(GroupStatus.ACTIVE);

        mentorScheduleValidator.validateMentorSchedule(request.getMentorId(), group.getWorkDays(), group.getWorkStartTime(),
                group.getWorkEndTime(), program, null);

        group = groupRepository.save(group);
        log.info("Created program group: {}", group.getName());

        return mapToResponse(group);
    }

    @Transactional
    public void assignIntern(Long groupId, Long internId) {
        ProgramGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group", groupId));

        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern profile", internId));

        if (memberRepository.existsByGroupIdAndInternId(groupId, internId)) {
            throw new ConflictException("Thực tập sinh đã được phân vào nhóm này rồi");
        }

        // BR-06: Check mentor capacity constraint (max 10 active interns per mentor)
        if (group.getMentorId() != null) {
            long currentCount = memberRepository.countActiveMembersByMentorId(group.getMentorId());
            if (currentCount >= 10) {
                throw new BadRequestException("Mentor đã quản lý tối đa 10 thực tập sinh (BR-06). Không thể phân bổ thêm.");
            }
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

        // Auto-assign group mentor to intern if group has a valid mentor
        if (group.getMentorId() != null && mentorRepository.existsById(group.getMentorId())) {
            boolean alreadyAssigned = intern.getMentor() != null && intern.getMentor().getId().equals(group.getMentorId());
            if (!alreadyAssigned) {
                try {
                    mentorAssignmentService.assignMentorToIntern(internId, group.getMentorId(), "Phân công theo nhóm: " + group.getName());
                } catch (Exception e) {
                    log.warn("Auto-assign mentor {} to intern {} failed: {}", group.getMentorId(), internId, e.getMessage());
                }
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
                .orElseThrow(() -> new NotFoundException("Group", id));
        return mapToResponse(group);
    }

    @Transactional(readOnly = true)
    public List<com.holaho.intern.shared.dto.response.GroupMemberResponse> getMembers(Long groupId) {
        return memberRepository.findByGroupId(groupId).stream()
                .map(member -> com.holaho.intern.shared.dto.response.GroupMemberResponse.builder()
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
    public GroupResponse update(Long id, com.holaho.intern.shared.dto.request.UpdateGroupRequest request) {
        ProgramGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Group", id));

        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getMentorId() != null) {
            Long oldMentorId = group.getMentorId();
            group.setMentorId(request.getMentorId());

            if (!request.getMentorId().equals(oldMentorId)) {
                List<GroupMember> activeMembers = memberRepository.findActiveByGroupId(id);
                for (GroupMember member : activeMembers) {
                    try {
                        mentorAssignmentService.assignMentorToIntern(
                                member.getIntern().getId(),
                                request.getMentorId(),
                                "Cập nhật Mentor theo nhóm: " + group.getName()
                        );
                    } catch (Exception e) {
                        log.warn("Auto-assign updated mentor {} to intern {} failed: {}", request.getMentorId(), member.getIntern().getId(), e.getMessage());
                    }
                }
            }
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
            mentorScheduleValidator.validateMentorSchedule(group.getMentorId(), group.getWorkDays(), group.getWorkStartTime(),
                    group.getWorkEndTime(), group.getProgram(), id);
        }

        group = groupRepository.save(group);
        log.info("Updated program group: {}", id);
        return mapToResponse(group);
    }

    @Transactional
    public void delete(Long id) {
        if (!groupRepository.existsById(id)) {
            throw new NotFoundException("Group", id);
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
                .orElseThrow(() -> new NotFoundException("Thực tập sinh không tồn tại trong nhóm này"));

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
    public GroupResponse create(com.holaho.intern.shared.dto.request.GroupRequest request) {
        Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new NotFoundException("Program", request.getProgramId()));

        ProgramGroup group = new ProgramGroup();
        group.setProgram(program);
        group.setName(request.getName());
        group.setDepartmentId(request.getDepartmentId());
        group.setMentorId(request.getMentorId());
        group.setStatus(GroupStatus.ACTIVE);
        group.setWorkStartTime(request.getWorkStartTime());
        group.setWorkEndTime(request.getWorkEndTime());
        group.setWorkDays(request.getWorkDays());

        mentorScheduleValidator.validateMentorSchedule(request.getMentorId(), group.getWorkDays(), group.getWorkStartTime(),
                group.getWorkEndTime(), program, null);

        group = groupRepository.save(group);
        log.info("Created program group: {}", group.getName());

        return mapToResponse(group);
    }

    private GroupResponse mapToResponse(ProgramGroup group) {
        GroupResponse response = new GroupResponse(group);

        List<GroupMember> members = memberRepository.findByGroupIdWithIntern(group.getId());
        response.setTotalMembers((long) members.size());

        List<GroupMemberResponse> memberResponses = members.stream()
                .map(m -> GroupMemberResponse.builder()
                        .id(m.getId())
                        .groupId(m.getGroup().getId())
                        .internId(m.getIntern().getId())
                        .internName(m.getIntern().getUser() != null ? m.getIntern().getUser().getFullName() : null)
                        .internEmail(m.getIntern().getUser() != null ? m.getIntern().getUser().getEmail() : null)
                        .studentCode(m.getIntern().getStudentCode())
                        .joinedAt(m.getJoinedAt())
                        .build())
                .collect(Collectors.toList());
        response.setMembers(memberResponses);

        if (group.getMentorId() != null) {
            mentorRepository.findByIdWithUser(group.getMentorId()).ifPresent(mentor -> {
                if (mentor.getUser() != null) {
                    response.setMentorName(mentor.getUser().getFullName());
                }
            });
        }

        return response;
    }

    @Transactional(readOnly = true)
    public com.holaho.intern.shared.dto.response.MyGroupInfoResponse getMyGroupInfoByUserId(Long userId) {
        InternProfile intern = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Intern profile for user ID", userId));

        Optional<GroupMember> membershipOpt = memberRepository.findFirstByIntern_IdAndLeftAtIsNull(intern.getId());
        if (membershipOpt.isEmpty()) {
            return com.holaho.intern.shared.dto.response.MyGroupInfoResponse.builder()
                    .coInterns(List.of())
                    .build();
        }

        GroupMember currentMember = membershipOpt.get();
        ProgramGroup group = currentMember.getGroup();
        Program program = group.getProgram();

        com.holaho.intern.shared.dto.response.MyGroupInfoResponse res = com.holaho.intern.shared.dto.response.MyGroupInfoResponse.builder()
                .groupId(group.getId())
                .groupName(group.getName())
                .groupDescription(group.getDescription())
                .build();

        if (program != null) {
            res.setProgramId(program.getId());
            res.setProgramName(program.getName());
            res.setProgramCode(program.getCode());
            res.setProgramStartDate(program.getStartDate());
            res.setProgramEndDate(program.getEndDate());
        }

        if (group.getMentorId() != null) {
            res.setMentorId(group.getMentorId());
            mentorRepository.findByIdWithUser(group.getMentorId()).ifPresent(m -> {
                if (m.getUser() != null) {
                    res.setMentorName(m.getUser().getFullName());
                    res.setMentorEmail(m.getUser().getEmail());
                    res.setMentorPhone(m.getUser().getPhone());
                }
                if (m.getDepartment() != null) {
                    res.setMentorDepartment(m.getDepartment().getName());
                }
            });
        }

        List<GroupMember> groupMembers = memberRepository.findByGroupIdWithIntern(group.getId());
        List<com.holaho.intern.shared.dto.response.MyGroupInfoResponse.CoInternDto> coInterns = groupMembers.stream()
                .map(gm -> com.holaho.intern.shared.dto.response.MyGroupInfoResponse.CoInternDto.builder()
                        .internId(gm.getIntern().getId())
                        .fullName(gm.getIntern().getUser() != null ? gm.getIntern().getUser().getFullName() : null)
                        .studentCode(gm.getIntern().getStudentCode())
                        .email(gm.getIntern().getUser() != null ? gm.getIntern().getUser().getEmail() : null)
                        .university(gm.getIntern().getUniversity())
                        .major(gm.getIntern().getMajor())
                        .joinedAt(gm.getJoinedAt())
                        .build())
                .collect(Collectors.toList());

        res.setCoInterns(coInterns);
        return res;
    }
}
