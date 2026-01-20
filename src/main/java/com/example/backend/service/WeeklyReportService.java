package com.example.backend.service;

import com.example.backend.dto.request.WeeklyReportRequest;
import com.example.backend.entity.*;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyReportService {

        private final WeeklyReportRepository reportRepository;
        private final EvaluationRepository evaluationRepository;
        private final InternProfileRepository internRepository;
        private final UserRepository userRepository;
        private final MentorRepository mentorRepository;

        @Transactional(readOnly = true)
        public com.example.backend.dto.response.FinalReportDto getFinalReport(Long internId) {
                InternProfile intern = internRepository.findById(internId)
                                .orElseThrow(() -> new NotFoundException("Intern profile not found"));

                List<Evaluation> evaluations = evaluationRepository.findByInternId(internId);
                List<WeeklyReport> reports = reportRepository.findByIntern_IdOrderByWeekNumberDesc(internId);

                List<com.example.backend.dto.response.EvaluationResponse> evaluationDtos = evaluations.stream()
                                .map(e -> com.example.backend.dto.response.EvaluationResponse.builder()
                                                .id(e.getId())
                                                .internId(e.getIntern().getId())
                                                .internName(e.getIntern().getUser().getFullName())
                                                .mentorId(e.getMentor().getId())
                                                .mentorName(e.getMentor().getUser().getFullName())
                                                .period(e.getPeriod())
                                                .score(e.getScore())
                                                .comment(e.getComment())
                                                .createdAt(e.getCreatedAt())
                                                .build())
                                .toList();

                List<com.example.backend.dto.WeeklyReportDto> reportDtos = reports.stream()
                                .map(this::mapToDto)
                                .toList();

                Double avgScore = evaluations.stream()
                                .mapToInt(e -> e.getScore() != null ? e.getScore() : 0)
                                .average()
                                .orElse(0.0);

                String assessment = "Chưa đánh giá";
                if (!evaluations.isEmpty()) {
                        if (avgScore >= 9.0)
                                assessment = "Xuất sắc";
                        else if (avgScore >= 8.0)
                                assessment = "Giỏi";
                        else if (avgScore >= 6.5)
                                assessment = "Khá";
                        else if (avgScore >= 5.0)
                                assessment = "Trung bình";
                        else
                                assessment = "Yếu";
                }

                return com.example.backend.dto.response.FinalReportDto.builder()
                                .internId(intern.getId())
                                .fullName(intern.getUser().getFullName())
                                .studentCode(intern.getStudentCode())
                                .university(intern.getUniversity())
                                .major(intern.getMajor())
                                .email(intern.getUser().getEmail())
                                .startDate(intern.getStartDate())
                                .endDate(intern.getEndDate())
                                .mentorName(intern.getMentor() != null ? intern.getMentor().getUser().getFullName()
                                                : "N/A")
                                .groupName("N/A")
                                .evaluations(evaluationDtos)
                                .weeklyReports(reportDtos)
                                .finalScore(Math.round(avgScore * 100.0) / 100.0)
                                .finalAssessment(assessment)
                                .totalReports(reports.size())
                                .build();
        }

        @Transactional(readOnly = true)
        public List<com.example.backend.dto.WeeklyReportDto> getByIntern(Long internId) {
                return reportRepository.findByIntern_IdOrderByWeekNumberDesc(internId).stream()
                                .map(this::mapToDto)
                                .toList();
        }

        // --- Controller Support Methods ---

        @Transactional
        public com.example.backend.dto.WeeklyReportDto internSubmit(Long internId, WeeklyReportRequest req) {
                InternProfile intern = internRepository.findById(internId)
                                .orElseThrow(() -> new NotFoundException("Intern profile not found"));

                WeeklyReport report = new WeeklyReport();
                report.setIntern(intern);
                report.setWeekNumber(req.getWeekNumber());
                report.setTitle("Báo cáo tuần " + req.getWeekNumber());
                report.setWeekStart(req.getWeekStart());
                report.setWeekEnd(req.getWeekEnd());
                report.setReportDate(req.getReportDate());
                report.setCompletedWork(req.getCompletedWork());
                report.setPlannedWork(req.getPlannedWork());
                report.setChallenges(req.getChallenges());
                report.setLearnings(req.getLearnings());

                if (intern.getMentor() != null) {
                        report.setMentor(intern.getMentor().getUser());
                }

                // Use string for status if enum not available or map correctly
                report.setStatus("SUBMITTED");
                report.setSubmittedAt(LocalDateTime.now());

                report = reportRepository.save(report);
                log.info("Intern {} submitted report for week {}", internId, req.getWeekNumber());
                return mapToDto(report);
        }

        @Transactional(readOnly = true)
        public org.springframework.data.domain.Page<com.example.backend.dto.WeeklyReportDto> internMyReports(
                        Long internId, org.springframework.data.domain.Pageable pageable) {
                return reportRepository.findByIntern_Id(internId, pageable)
                                .map(this::mapToDto);
        }

        @Transactional(readOnly = true)
        public org.springframework.data.domain.Page<com.example.backend.dto.WeeklyReportDto> mentorGroupReports(
                        Long mentorUserId, Long groupId, Long internId, String status,
                        org.springframework.data.domain.Pageable pageable) {

                // Get Mentor Profile ID from User ID
                Mentor mentor = mentorRepository.findByUser_Id(mentorUserId)
                                .orElseThrow(() -> new NotFoundException(
                                                "Mentor profile not found for user: " + mentorUserId));

                // Query using Mentor Profile ID and Filters
                return reportRepository.findReportsForMentor(
                                mentor.getId(),
                                com.example.backend.enums.GroupStatus.ACTIVE,
                                internId,
                                status != null && !status.isEmpty() ? status : null,
                                pageable)
                                .map(this::mapToDto);
        }

        @Transactional
        public com.example.backend.dto.WeeklyReportDto mentorReview(
                        Long mentorUserId, Long reportId,
                        com.example.backend.dto.request.ReviewWeeklyReportRequest req) {

                WeeklyReport report = reportRepository.findById(reportId)
                                .orElseThrow(() -> new NotFoundException("Report not found"));

                User mentor = userRepository.findById(mentorUserId)
                                .orElseThrow(() -> new NotFoundException("Mentor user not found"));

                report.setMentor(mentor);
                report.setMentor(mentor);
                report.setMentorFeedback(req.feedback());
                if (req.rating() != null) {
                        report.setRating(req.rating());
                }
                report.setStatus("REVIEWED");
                report.setReviewedAt(LocalDateTime.now());

                report = reportRepository.save(report);
                return mapToDto(report);
        }

        @Transactional(readOnly = true)
        public com.example.backend.dto.WeeklyReportDto getReportDetail(Long id) {
                WeeklyReport report = reportRepository.findById(id)
                                .orElseThrow(() -> new NotFoundException("Report not found"));
                return mapToDto(report);
        }

        @Transactional
        public com.example.backend.dto.WeeklyReportDto updateStatus(Long id, String status) {
                WeeklyReport report = reportRepository.findById(id)
                                .orElseThrow(() -> new NotFoundException("Report not found"));
                report.setStatus(status);
                if ("REVIEWED".equals(status)) {
                        report.setReviewedAt(LocalDateTime.now());
                }
                return mapToDto(reportRepository.save(report));
        }

        @Transactional(readOnly = true)
        public List<com.example.backend.dto.response.FinalReportSummaryDto> getReportsSummary() {
                List<InternProfile> interns = internRepository.findAll();

                return interns.stream().map(intern -> {
                        List<Evaluation> evaluations = evaluationRepository.findByInternId(intern.getId());
                        long reportCount = reportRepository.countByIntern_Id(intern.getId());

                        Double avgScore = evaluations.stream()
                                        .mapToInt(e -> e.getScore() != null ? e.getScore() : 0)
                                        .average()
                                        .orElse(0.0);

                        String assessment = "Chưa đánh giá";
                        if (!evaluations.isEmpty()) {
                                if (avgScore >= 9.0)
                                        assessment = "Xuất sắc";
                                else if (avgScore >= 8.0)
                                        assessment = "Giỏi";
                                else if (avgScore >= 6.5)
                                        assessment = "Khá";
                                else if (avgScore >= 5.0)
                                        assessment = "Trung bình";
                                else
                                        assessment = "Yếu";
                        }

                        return com.example.backend.dto.response.FinalReportSummaryDto.builder()
                                        .internId(intern.getId())
                                        .fullName(intern.getUser().getFullName())
                                        .studentCode(intern.getStudentCode())
                                        .university(intern.getUniversity())
                                        .mentorName(intern.getMentor() != null
                                                        ? intern.getMentor().getUser().getFullName()
                                                        : "N/A")
                                        .finalScore(Math.round(avgScore * 100.0) / 100.0)
                                        .finalAssessment(assessment)
                                        .reportCount((int) reportCount)
                                        .build();
                }).toList();
        }

        @Transactional(readOnly = true)
        public List<com.example.backend.dto.InternCountStatDto> getStatsByAssessment() {
                List<com.example.backend.dto.response.FinalReportSummaryDto> summaries = getReportsSummary();

                // Group by assessment and count
                java.util.Map<String, Long> counts = summaries.stream()
                                .collect(java.util.stream.Collectors.groupingBy(
                                                com.example.backend.dto.response.FinalReportSummaryDto::getFinalAssessment,
                                                java.util.stream.Collectors.counting()));

                return counts.entrySet().stream()
                                .map(e -> new com.example.backend.dto.InternCountStatDto(e.getKey(), e.getValue()))
                                .sorted((a, b) -> Long.compare(b.count(), a.count())) // Sort by count desc
                                .toList();
        }

        private com.example.backend.dto.WeeklyReportDto mapToDto(WeeklyReport report) {
                User mentorUser = report.getMentor();
                if (mentorUser == null && report.getIntern().getMentor() != null) {
                        mentorUser = report.getIntern().getMentor().getUser();
                }

                return new com.example.backend.dto.WeeklyReportDto(
                                report.getId(),
                                report.getIntern().getId(),
                                report.getIntern().getUser().getFullName(),
                                report.getWeekNumber(),
                                report.getWeekStart(),
                                report.getWeekEnd(),
                                report.getReportDate(),
                                report.getCompletedWork(),
                                report.getPlannedWork(),
                                report.getChallenges(),
                                report.getLearnings(),
                                report.getLearnings(), // summary alias
                                com.example.backend.enums.WeeklyReportStatus.valueOf(report.getStatus()), // Ensure Enum
                                                                                                          // matches
                                                                                                          // string
                                                                                                          // in DB
                                report.getMentorFeedback(),
                                report.getRating(),
                                mentorUser != null ? mentorUser.getId() : null,
                                mentorUser != null ? mentorUser.getFullName() : null,
                                report.getCreatedAt(),
                                report.getReviewedAt());
        }
}
