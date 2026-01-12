package com.example.backend.service.impl;

import com.example.backend.dto.response.AssignMentorResponse;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Mentor;
import com.example.backend.enums.UserStatus;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.MentorRepository;
import com.example.backend.service.MentorAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MentorAssignmentServiceImpl implements MentorAssignmentService {

    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;

    public MentorAssignmentServiceImpl(
            InternProfileRepository internProfileRepository,
            MentorRepository mentorRepository
    ) {
        this.internProfileRepository = internProfileRepository;
        this.mentorRepository = mentorRepository;
    }

    @Override
    @Transactional
    public AssignMentorResponse assignMentorToIntern(Long internId, Long mentorId) {
        if (internId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "internId is required");
        }
        if (mentorId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "mentorId is required");
        }

        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern not found: " + internId));

        if (intern.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Intern user is not active");
        }

        Mentor mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentor not found: " + mentorId));

        if (mentor.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mentor user is not active");
        }

        intern.setMentor(mentor);
        InternProfile saved = internProfileRepository.save(intern);

        return toResponse(saved, LocalDateTime.now());
    }

    @Override
    @Transactional
    public AssignMentorResponse removeMentorFromIntern(Long internId) {
        if (internId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "internId is required");
        }

        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern not found: " + internId));

        intern.setMentor(null);
        InternProfile saved = internProfileRepository.save(intern);

        return toResponse(saved, LocalDateTime.now());
    }

    private AssignMentorResponse toResponse(InternProfile intern, LocalDateTime assignedAt) {
        AssignMentorResponse res = new AssignMentorResponse();
        res.setInternId(intern.getId());

        if (intern.getMentor() != null) {
            res.setMentorId(intern.getMentor().getId());
            if (intern.getMentor().getUser() != null) {
                res.setMentorEmail(intern.getMentor().getUser().getEmail());
                res.setMentorFullName(intern.getMentor().getUser().getFullName());
            }
            if (intern.getMentor().getDepartment() != null) {
                res.setMentorDepartmentId(intern.getMentor().getDepartment().getId());
                res.setMentorDepartmentName(intern.getMentor().getDepartment().getName());
            }
        }

        res.setAssignedAt(assignedAt);
        return res;
    }
}
