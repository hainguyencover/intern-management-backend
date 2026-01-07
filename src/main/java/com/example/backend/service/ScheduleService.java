package com.example.backend.service;

import com.example.backend.dto.response.InternScheduleResponse;
import com.example.backend.entity.GroupMember;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Program;
import com.example.backend.entity.ProgramGroup;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.GroupMemberRepository;
import com.example.backend.repository.InternProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final InternProfileRepository internProfileRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional(readOnly = true)
    public InternScheduleResponse getMySchedule(String email) {

        InternProfile intern = internProfileRepository.findByUser_Email(email)
                .orElseThrow(() ->
                        new NotFoundException("Intern profile not found for user: " + email)
                );

        GroupMember member = groupMemberRepository
                .findFirstByIntern_IdAndLeftAtIsNull(intern.getId())
                .orElseThrow(() ->
                        new NotFoundException("Intern is not assigned to any active group")
                );

        ProgramGroup group = member.getGroup();
        Program program = group.getProgram();

        return new InternScheduleResponse(
                intern.getId(),
                intern.getUser().getFullName(),
                intern.getStartDate(),
                intern.getEndDate(),
                program.getId(),
                program.getName(),
                program.getStartDate(),
                program.getEndDate(),
                group.getId(),
                group.getName()
        );
    }
}

