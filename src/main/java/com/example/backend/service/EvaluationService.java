// service/EvaluationService.java
package com.example.backend.service;

import com.example.backend.dto.request.EvaluationRequest;
import com.example.backend.entity.*;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

        private final EvaluationRepository evaluationRepository;
        private final InternProfileRepository internRepository;
        private final MentorRepository mentorRepository;

        @Transactional
        public com.example.backend.dto.response.EvaluationResponse create(EvaluationRequest req, Long mentorUserId) {
                InternProfile intern = internRepository.findById(req.getInternId())
                                .orElseThrow(() -> new NotFoundException("Intern not found"));

                Mentor mentor = mentorRepository.findByUser_Id(mentorUserId)
                                .orElseThrow(() -> new NotFoundException("Mentor profile not found"));

                Evaluation eval = new Evaluation();
                eval.setIntern(intern);
                eval.setMentor(mentor);
                eval.setPeriod(req.getPeriod());
                eval.setScore(req.getScore());
                eval.setComment(req.getComment());

                eval = evaluationRepository.save(eval);
                log.info("Created evaluation: {}", eval.getId());

                return mapToResponse(eval);
        }

        @Transactional(readOnly = true)
        public List<com.example.backend.dto.response.EvaluationResponse> getByIntern(Long internId) {
                return evaluationRepository.findByInternId(internId).stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public com.example.backend.dto.response.EvaluationResponse getById(Long id) {
                Evaluation eval = evaluationRepository.findById(id)
                                .orElseThrow(() -> new NotFoundException("Evaluation not found: " + id));
                return mapToResponse(eval);
        }

        @Transactional(readOnly = true)
        public List<com.example.backend.dto.response.EvaluationResponse> getInternEvaluations(Long userId) {
                InternProfile intern = internRepository.findByUser_Id(userId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Intern profile not found for user: " + userId));
                return evaluationRepository.findByInternId(intern.getId()).stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public org.springframework.data.domain.Page<com.example.backend.dto.response.EvaluationResponse> getMentorEvaluations(
                        Long userId, String period, String keyword, org.springframework.data.domain.Pageable pageable) {
                Mentor mentor = mentorRepository.findByUser_Id(userId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Mentor profile not found for user: " + userId));
                return evaluationRepository.findByMentorIdAndFilters(mentor.getId(),
                                period != null && !period.isEmpty() ? period : null,
                                keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null,
                                pageable)
                                .map(this::mapToResponse);
        }

        private com.example.backend.dto.response.EvaluationResponse mapToResponse(Evaluation eval) {
                return com.example.backend.dto.response.EvaluationResponse.builder()
                                .id(eval.getId())
                                .internId(eval.getIntern().getId())
                                .internName(eval.getIntern().getUser().getFullName())
                                .mentorId(eval.getMentor().getId())
                                .mentorName(eval.getMentor().getUser().getFullName())
                                .period(eval.getPeriod())
                                .score(eval.getScore())
                                .comment(eval.getComment())
                                .createdAt(eval.getCreatedAt())
                                .build();
        }
}
