package com.example.backend.service;

import com.example.backend.dto.request.AssignInternRequest;
import com.example.backend.dto.request.CreateGroupRequest;
import com.example.backend.dto.request.ProgramGroupCreateRequest;
import com.example.backend.dto.response.GroupResponse;
import com.example.backend.dto.response.MemberResponse;
import com.example.backend.dto.response.ProgramGroupResponse;
import com.example.backend.entity.GroupMember;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Mentor;
import com.example.backend.entity.Program;
import com.example.backend.entity.ProgramGroup;
import com.example.backend.enums.ApplicationStatus;
import com.example.backend.enums.GroupStatus;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProgramGroupService {

    private final ProgramGroupRepository programGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ProgramRepository programRepository;
    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;
    private final ApplicationRepository applicationRepository;

    /**
     * API cũ: /api/program-groups
     * -> Trả ProgramGroupResponse để không phá controller cũ
     */
    @Transactional
    public ProgramGroupResponse createGroup(ProgramGroupCreateRequest req) {
        Program program = programRepository.findById(req.getProgramId())
                .orElseThrow(() -> new NotFoundException("Program not found: " + req.getProgramId()));

        // mentorId trong req hiện đang dùng để set group.mentorId
        Mentor mentor = mentorRepository.findById(req.getMentorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mentor not found"));

        ProgramGroup group = new ProgramGroup();
        group.setProgram(program);

        // mentorId: bắt buộc
        group.setMentorId(req.getMentorId());

        // departmentId: ưu tiên req (nếu có), không thì lấy từ program.department
        Long deptId = req.getDepartmentId() != null
                ? req.getDepartmentId()
                : (program.getDepartment() != null ? program.getDepartment().getId() : null);
        group.setDepartmentId(deptId);

        // name: nếu null/blank thì default
        String name = (req.getName() == null || req.getName().isBlank())
                ? ("Group - " + safeFullName(mentor))
                : req.getName().trim();
        group.setName(name);

        group.setStatus(GroupStatus.ACTIVE);

        ProgramGroup saved = programGroupRepository.save(group);
        return toProgramGroupResponse(saved);
    }

    /**
     * API HR mới: /api/hr/programs/{programId}/groups
     * -> Trả GroupResponse (có mentorName + memberCount)
     */
    @Transactional
    public GroupResponse createGroupHr(Long programId, CreateGroupRequest req) {
        ProgramGroupCreateRequest old = new ProgramGroupCreateRequest();
        old.setProgramId(programId);
        old.setMentorId(req.getMentorId());
        old.setName(req.getName());
        // departmentId optional: nếu bạn muốn set theo program thì để null
        old.setDepartmentId(null);

        // tạo group (tạo thật)
        ProgramGroupResponse created = createGroup(old);

        // trả về DTO HR (đủ info)
        ProgramGroup g = programGroupRepository.findById(created.getId())
                .orElseThrow(() -> new NotFoundException("Program group not found: " + created.getId()));

        Mentor mentor = mentorRepository.findById(g.getMentorId()).orElse(null);
        return toGroupResponse(g, mentor);
    }

    public List<GroupResponse> listGroups(Long programId) {
        programRepository.findById(programId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));

        List<ProgramGroup> groups = programGroupRepository.findByProgram_Id(programId);

        return groups.stream().map(g -> {
            Mentor mentor = null;
            if (g.getMentorId() != null) {
                mentor = mentorRepository.findById(g.getMentorId()).orElse(null);
            }
            return toGroupResponse(g, mentor);
        }).toList();
    }

    public List<MemberResponse> listMembers(Long groupId) {
        ProgramGroup group = programGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        return groupMemberRepository.findActiveMembersByGroupId(group.getId())
                .stream()
                .map(gm -> MemberResponse.builder()
                        .internId(gm.getIntern().getId())
                        .internName(safeFullName(gm.getIntern()))
                        .joinedAt(gm.getJoinedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void assignIntern(Long groupId, AssignInternRequest req) {
        ProgramGroup group = programGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Program group not found: " + groupId));

        InternProfile intern = internProfileRepository.findById(req.getInternId())
                .orElseThrow(() -> new NotFoundException("Intern not found: " + req.getInternId()));

        // eligibility
        boolean eligible = applicationRepository.existsByIntern_IdAndStatusIn(
                intern.getId(),
                Set.of(ApplicationStatus.APPROVED, ApplicationStatus.CONTRACT_SIGNED)
        );
        if (!eligible) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Intern is not eligible (Application must be APPROVED or CONTRACT_SIGNED)");
        }

        // chặn trùng trong group
        if (groupMemberRepository.existsActiveInGroup(group.getId(), intern.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intern already in this group");
        }

        // chặn trùng trong cùng program
        Long programId = group.getProgram().getId();
        if (groupMemberRepository.existsActiveInProgram(intern.getId(), programId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Intern already assigned to another group in this program");
        }

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setIntern(intern);
        member.setJoinedAt(LocalDateTime.now());
        member.setLeftAt(null);

        groupMemberRepository.save(member);
    }

    public void removeIntern(Long groupId, Long internId) {
        ProgramGroup group = programGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        GroupMember gm = groupMemberRepository.findActiveMembership(group.getId(), internId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in group"));

        gm.setLeftAt(LocalDateTime.now());
        groupMemberRepository.save(gm);
    }

    private GroupResponse toGroupResponse(ProgramGroup g, Mentor mentor) {
        long memberCount = groupMemberRepository.countActiveByGroupId(g.getId());
        return GroupResponse.builder()
                .id(g.getId())
                .programId(g.getProgram().getId())
                .departmentId(g.getDepartmentId())
                .mentorId(g.getMentorId())
                .mentorName(mentor == null ? null : safeFullName(mentor))
                .name(g.getName())
                .status(g.getStatus())
                .memberCount(memberCount)
                .build();
    }

    private ProgramGroupResponse toProgramGroupResponse(ProgramGroup g) {
        return ProgramGroupResponse.builder()
                .id(g.getId())
                .programId(g.getProgram().getId())
                .name(g.getName())
                .status(g.getStatus())
                .departmentId(g.getDepartmentId())
                .mentorId(g.getMentorId())
                .build();
    }

    private String safeFullName(Mentor mentor) {
        try {
            if (mentor != null && mentor.getUser() != null) return mentor.getUser().getFullName();
        } catch (Exception ignored) {
        }
        return "Mentor#" + (mentor == null ? "null" : mentor.getId());
    }

    private String safeFullName(InternProfile intern) {
        try {
            if (intern != null && intern.getUser() != null) return intern.getUser().getFullName();
        } catch (Exception ignored) {
        }
        return "Intern#" + (intern == null ? "null" : intern.getId());
    }
}
