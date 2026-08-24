package com.holaho.intern.service;

import com.holaho.intern.entity.GroupMember;
import com.holaho.intern.entity.InternshipEnrollment;
import com.holaho.intern.entity.Program;
import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.repository.MentorAssignmentRepository;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.repository.InternshipEnrollmentRepository;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.shared.dto.response.EnrollmentResponse;
import com.holaho.intern.shared.enums.EnrollmentStatus;
import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.holaho.intern.mentor.service.MentorAssignmentService;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternshipEnrollmentService {

    private final InternshipEnrollmentRepository enrollmentRepository;
    private final ProgramRepository programRepository;
    private final ProgramGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final InternProfileRepository internProfileRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final MentorAssignmentService mentorAssignmentService;

    @Transactional
    public EnrollmentResponse enrollIntern(Long programId, Long internId, Long groupId, LocalDate joinedAt) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program", programId));

        if (program.getStatus() == ProgramStatus.COMPLETED || program.getStatus() == ProgramStatus.CANCELLED) {
            throw new BadRequestException("Không thể tiếp nhận thực tập sinh vào chương trình đã kết thúc hoặc hủy");
        }

        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern profile", internId));

        // Check duplicate enrollment in same program
        if (enrollmentRepository.existsByProgramIdAndInternId(programId, internId)) {
            throw new ConflictException("Thực tập sinh đã tham gia chương trình này rồi");
        }

        // BR-02: Check single active program constraint
        Optional<InternshipEnrollment> existingActive = enrollmentRepository.findByInternIdAndStatus(internId, EnrollmentStatus.ACTIVE);
        if (existingActive.isPresent()) {
            throw new ConflictException("Thực tập sinh hiện đang tham gia chương trình '" + existingActive.get().getProgram().getName() + "'. Mỗi thực tập sinh chỉ thuộc 1 chương trình tại một thời điểm.");
        }

        // Check program capacity
        if (program.getMaxInterns() != null) {
            long currentCount = enrollmentRepository.findByProgramId(programId).stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                    .count();
            if (currentCount >= program.getMaxInterns()) {
                throw new BadRequestException("Chương trình '" + program.getName() + "' đã đạt sức chứa tối đa (" + program.getMaxInterns() + " thực tập sinh)");
            }
        }

        ProgramGroup group = null;
        if (groupId != null) {
            group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new NotFoundException("Group", groupId));
            if (!group.getProgram().getId().equals(programId)) {
                throw new BadRequestException("Nhóm chọn không thuộc chương trình này");
            }
        }

        InternshipEnrollment enrollment = new InternshipEnrollment();
        enrollment.setProgram(program);
        enrollment.setGroup(group);
        enrollment.setIntern(intern);
        enrollment.setJoinedAt(joinedAt != null ? joinedAt : LocalDate.now());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);

        enrollment = enrollmentRepository.save(enrollment);

        // Assign to group member table if group specified
        if (group != null) {
            if (!groupMemberRepository.existsByGroupIdAndInternId(group.getId(), internId)) {
                GroupMember gm = new GroupMember();
                gm.setGroup(group);
                gm.setIntern(intern);
                gm.setJoinedAt(LocalDateTime.now());
                groupMemberRepository.save(gm);
            }

            if (group.getMentorId() != null) {
                try {
                    mentorAssignmentService.assignMentorToIntern(internId, group.getMentorId(), "Tiếp nhận vào nhóm: " + group.getName());
                } catch (Exception ex) {
                    log.warn("Auto-assign mentor {} to intern {} failed during enrollment: {}", group.getMentorId(), internId, ex.getMessage());
                }
            }
        }

        // Update intern status to ACTIVE or ONBOARDING
        if (!"ACTIVE".equals(intern.getStatus())) {
            intern.setStatus("ACTIVE");
            internProfileRepository.save(intern);
        }

        log.info("Enrolled intern {} into program {}", internId, programId);
        return mapToResponse(enrollment);
    }

    @Transactional
    public void withdrawEnrollment(Long enrollmentId) {
        InternshipEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment", enrollmentId));

        enrollment.setStatus(EnrollmentStatus.WITHDRAWN);
        enrollment.setEndedAt(LocalDate.now());
        enrollmentRepository.save(enrollment);

        log.info("Withdrew enrollment id {}", enrollmentId);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getProgramEnrollments(Long programId, String keyword, Long groupId, Pageable pageable) {
        List<InternshipEnrollment> list = enrollmentRepository.findByProgramId(programId);

        List<EnrollmentResponse> responses = list.stream()
                .filter(e -> {
                    if (groupId != null && (e.getGroup() == null || !e.getGroup().getId().equals(groupId))) {
                        return false;
                    }
                    if (keyword != null && !keyword.isBlank()) {
                        String k = keyword.toLowerCase();
                        String name = e.getIntern().getUser() != null ? e.getIntern().getUser().getFullName().toLowerCase() : "";
                        String code = e.getIntern().getStudentCode() != null ? e.getIntern().getStudentCode().toLowerCase() : "";
                        return name.contains(k) || code.contains(k);
                    }
                    return true;
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), responses.size());
        List<EnrollmentResponse> pageContent = (start <= end && start < responses.size()) ? responses.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, pageable, responses.size());
    }

    private EnrollmentResponse mapToResponse(InternshipEnrollment e) {
        EnrollmentResponse res = new EnrollmentResponse();
        res.setId(e.getId());
        res.setProgramId(e.getProgram().getId());
        res.setProgramName(e.getProgram().getName());
        res.setProgramCode(e.getProgram().getCode());
        res.setInternId(e.getIntern().getId());
        res.setInternName(e.getIntern().getUser() != null ? e.getIntern().getUser().getFullName() : null);
        res.setInternEmail(e.getIntern().getUser() != null ? e.getIntern().getUser().getEmail() : null);
        res.setStudentCode(e.getIntern().getStudentCode());
        res.setUniversity(e.getIntern().getUniversity());
        res.setMajor(e.getIntern().getMajor());

        if (e.getGroup() != null) {
            res.setGroupId(e.getGroup().getId());
            res.setGroupName(e.getGroup().getName());
        }

        mentorAssignmentRepository.findByInternIdAndStatus(e.getIntern().getId(), MentorAssignmentStatus.ACTIVE)
                .ifPresent(ma -> {
                    res.setMentorId(ma.getMentor().getId());
                    if (ma.getMentor().getUser() != null) {
                        res.setMentorName(ma.getMentor().getUser().getFullName());
                    }
                });

        res.setStatus(e.getStatus());
        res.setJoinedAt(e.getJoinedAt());
        res.setEndedAt(e.getEndedAt());
        return res;
    }
}
