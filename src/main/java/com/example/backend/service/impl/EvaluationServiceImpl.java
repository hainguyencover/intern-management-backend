package com.example.backend.service.impl;

import com.example.backend.dto.request.UpsertEvaluationRequest;
import com.example.backend.dto.response.EvaluationResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.Evaluation;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Mentor;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.EvaluationRepository;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.MentorRepository;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;

    private Mentor currentMentor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();

        if (principal instanceof CustomUserDetails cud) {
            return mentorRepository.findByUser_Id(cud.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Mentor not found for current user"));
        }

        // fallback nếu principal không phải CustomUserDetails
        String email = auth.getName();
        return mentorRepository.findByUser_Email(email)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Mentor not found for current user"));
    }


    private static Integer calcOverall(UpsertEvaluationRequest req) {
        if (req.getOverallScore() != null) return req.getOverallScore();
        return Math.round((req.getSkillScore() + req.getAttitudeScore()) / 2.0f);
    }

    private static EvaluationResponse toResponse(Evaluation e) {
        return EvaluationResponse.builder()
                .id(e.getId())
                .internId(e.getIntern().getId())
                .internName(e.getIntern().getUser().getFullName())
                .mentorId(e.getMentor().getId())
                .mentorName(e.getMentor().getUser().getFullName())
                .period(e.getPeriod())
                .skillScore(e.getSkillScore())
                .attitudeScore(e.getAttitudeScore())
                .overallScore(e.getOverallScore())
                .comment(e.getComment())
                .build();
    }

    @Override
    @Transactional
    public EvaluationResponse upsert(UpsertEvaluationRequest req) {
        Mentor mentor = currentMentor();

        // intern phải thuộc mentor
        if (!internProfileRepository.existsByIdAndMentor_Id(req.getInternId(), mentor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not assigned to this intern");
        }

        InternProfile intern = internProfileRepository.findById(req.getInternId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern not found"));

        Evaluation eval = evaluationRepository
                .findByIntern_IdAndMentor_IdAndPeriod(intern.getId(), mentor.getId(), req.getPeriod())
                .orElseGet(Evaluation::new);

        eval.setIntern(intern);
        eval.setMentor(mentor);
        eval.setPeriod(req.getPeriod());
        eval.setSkillScore(req.getSkillScore());
        eval.setAttitudeScore(req.getAttitudeScore());
        eval.setOverallScore(calcOverall(req));
        eval.setComment(req.getComment());

        return toResponse(evaluationRepository.save(eval));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EvaluationResponse> myEvaluations(Pageable pageable) {
        Mentor mentor = currentMentor();
        Page<EvaluationResponse> page = evaluationRepository.findByMentor_Id(mentor.getId(), pageable)
                .map(EvaluationServiceImpl::toResponse);
        return PageResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EvaluationResponse> evaluationsOfIntern(Long internId, Pageable pageable) {
        Page<EvaluationResponse> page = evaluationRepository.findByIntern_Id(internId, pageable)
                .map(EvaluationServiceImpl::toResponse);
        return PageResponse.of(page);
    }
}
