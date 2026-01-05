package com.example.backend.service;

import com.example.backend.dto.request.AssignInternRequest;
import com.example.backend.dto.request.ProgramGroupCreateRequest;
import com.example.backend.dto.response.ProgramGroupResponse;
import com.example.backend.entity.GroupMember;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Program;
import com.example.backend.entity.ProgramGroup;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.GroupMemberRepository;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.ProgramGroupRepository;
import com.example.backend.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProgramGroupService {

    private final ProgramGroupRepository programGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ProgramRepository programRepository;
    private final InternProfileRepository internProfileRepository;

    @Transactional
    public ProgramGroupResponse createGroup(ProgramGroupCreateRequest req) {
        Program program = programRepository.findById(req.getProgramId())
                .orElseThrow(() -> new NotFoundException("Program not found: " + req.getProgramId()));

        ProgramGroup group = new ProgramGroup();
        group.setProgram(program);
        group.setName(req.getName());
        group.setStatus(req.getStatus());
        group.setDepartmentId(req.getDepartmentId()); // nullable
        group.setMentorId(req.getMentorId());         // nullable

        ProgramGroup saved = programGroupRepository.save(group);
        return toResponse(saved);
    }

    @Transactional
    public void assignIntern(Long groupId, AssignInternRequest req) {
        ProgramGroup group = programGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Program group not found: " + groupId));

        InternProfile intern = internProfileRepository.findById(req.getInternId())
                .orElseThrow(() -> new NotFoundException("Intern not found: " + req.getInternId()));

        // Nếu intern đang thuộc group khác (active) -> đóng membership cũ
        groupMemberRepository.findFirstByInternIdAndLeftAtIsNull(req.getInternId())
                .ifPresent(old -> {
                    old.setLeftAt(LocalDateTime.now());
                    groupMemberRepository.save(old);
                });

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setIntern(intern);
        member.setJoinedAt(LocalDateTime.now());
        member.setLeftAt(null);

        groupMemberRepository.save(member);
    }

    private ProgramGroupResponse toResponse(ProgramGroup g) {
        return new ProgramGroupResponse(
                g.getId(),
                g.getProgram().getId(),
                g.getName(),
                g.getStatus(),
                g.getDepartmentId(),
                g.getMentorId()
        );
    }
}
