//package com.example.backend.service.impl;
//
//import com.example.backend.dto.response.AssignMentorResponse;
//import com.example.backend.entity.InternProfile;
//import com.example.backend.entity.Mentor;
//import com.example.backend.exception.ApiException;
//import com.example.backend.repository.InternProfileRepository;
//import com.example.backend.repository.MentorRepository;
//import com.example.backend.service.MentorAssignmentService;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//
//@Service
//public class MentorAssignmentServiceImpl implements MentorAssignmentService {
//
//    private final InternProfileRepository internProfileRepository;
//    private final MentorRepository mentorRepository;
//
//    public MentorAssignmentServiceImpl(
//            InternProfileRepository internProfileRepository,
//            MentorRepository mentorRepository
//    ) {
//        this.internProfileRepository = internProfileRepository;
//        this.mentorRepository = mentorRepository;
//    }
//
//    @Override
//    @Transactional
//    public AssignMentorResponse assignMentorToIntern(Long internId, Long mentorId) {
//        if (mentorId == null) {
//            throw new ApiException(HttpStatus.BAD_REQUEST, "mentorId is required");
//        }
//
//        InternProfile intern = internProfileRepository.findById(internId)
//                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern not found: " + internId));
//
//        Mentor mentor = mentorRepository.findById(mentorId)
//                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentor not found: " + mentorId));
//
//        intern.setMentor(mentor);
//        InternProfile saved = internProfileRepository.save(intern);
//
//        return toResponse(saved, LocalDateTime.now());
//    }
//
//    @Override
//    @Transactional
//    public AssignMentorResponse removeMentorFromIntern(Long internId) {
//        InternProfile intern = internProfileRepository.findById(internId)
//                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern not found: " + internId));
//
//        intern.setMentor(null);
//        InternProfile saved = internProfileRepository.save(intern);
//
//        AssignMentorResponse res = new AssignMentorResponse();
//        res.setInternId(saved.getId());
//        res.setMentorId(null);
//        res.setAssignedAt(LocalDateTime.now());
//        return res;
//    }
//
//    private AssignMentorResponse toResponse(InternProfile intern, LocalDateTime assignedAt) {
//        AssignMentorResponse res = new AssignMentorResponse();
//        res.setInternId(intern.getId());
//
//        if (intern.getMentor() != null) {
//            res.setMentorId(intern.getMentor().getId());
//            if (intern.getMentor().getUser() != null) {
//                res.setMentorEmail(intern.getMentor().getUser().getEmail());
//                res.setMentorFullName(intern.getMentor().getUser().getFullName());
//            }
//        }
//
//        res.setAssignedAt(assignedAt);
//        return res;
//    }
//
//}

package com.example.backend.service.impl;

import com.example.backend.dto.response.AssignMentorResponse;
import com.example.backend.entity.GroupMember;
import com.example.backend.entity.Mentor;
import com.example.backend.entity.ProgramGroup;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.GroupMemberRepository;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.MentorRepository;
import com.example.backend.repository.ProgramGroupRepository;
import com.example.backend.service.MentorAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MentorAssignmentServiceImpl implements MentorAssignmentService {

    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ProgramGroupRepository programGroupRepository;

    public MentorAssignmentServiceImpl(
            InternProfileRepository internProfileRepository,
            MentorRepository mentorRepository,
            GroupMemberRepository groupMemberRepository,
            ProgramGroupRepository programGroupRepository
    ) {
        this.internProfileRepository = internProfileRepository;
        this.mentorRepository = mentorRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.programGroupRepository = programGroupRepository;
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

        // 1) validate intern exists
        if (!internProfileRepository.existsById(internId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Intern not found: " + internId);
        }

        // 2) validate mentor exists (để tránh gán id rác)
        Mentor mentor = mentorRepository.findById(mentorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mentor not found: " + mentorId));

        // 3) find active membership (intern must be in a group)
        GroupMember gm = groupMemberRepository.findFirstByIntern_IdAndLeftAtIsNull(internId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Intern chưa thuộc group (không có GroupMember active), không thể gán mentor"
                ));

        ProgramGroup group = gm.getGroup();
        if (group == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Group not found for intern: " + internId);
        }

        // 4) Assign mentorId to group
        group.setMentorId(mentorId);
        programGroupRepository.save(group);

        // 5) Response
        AssignMentorResponse res = new AssignMentorResponse();
        res.setInternId(internId);
        res.setMentorId(mentorId);
        res.setAssignedAt(LocalDateTime.now());

        if (mentor.getUser() != null) {
            res.setMentorEmail(mentor.getUser().getEmail());
            res.setMentorFullName(mentor.getUser().getFullName());
        }

        return res;
    }

    @Override
    @Transactional
    public AssignMentorResponse removeMentorFromIntern(Long internId) {
        if (internId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "internId is required");
        }

        // 1) validate intern exists
        if (!internProfileRepository.existsById(internId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Intern not found: " + internId);
        }

        // 2) find active membership
        GroupMember gm = groupMemberRepository.findFirstByIntern_IdAndLeftAtIsNull(internId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Intern chưa thuộc group (không có GroupMember active), không thể remove mentor"
                ));

        ProgramGroup group = gm.getGroup();
        if (group == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Group not found for intern: " + internId);
        }

        // 3) Remove mentorId from group
        group.setMentorId(null);
        programGroupRepository.save(group);

        // 4) Response
        AssignMentorResponse res = new AssignMentorResponse();
        res.setInternId(internId);
        res.setMentorId(null);
        res.setMentorEmail(null);
        res.setMentorFullName(null);
        res.setAssignedAt(LocalDateTime.now());
        return res;
    }
}
