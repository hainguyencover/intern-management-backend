package com.holaho.intern.service;

import com.holaho.intern.service.AiService;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.entity.Evaluation;
import com.holaho.intern.entity.WeeklyReport;
import com.holaho.intern.repository.EvaluationRepository;
import com.holaho.intern.repository.WeeklyReportRepository;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.enums.GroupStatus;

import com.holaho.intern.shared.dto.InternCountStatDto;
import com.holaho.intern.shared.dto.WeeklyReportDto;
import com.holaho.intern.shared.dto.request.ReviewWeeklyReportRequest;
import com.holaho.intern.shared.dto.request.WeeklyReportRequest;
import com.holaho.intern.shared.dto.response.EvaluationResponse;
import com.holaho.intern.shared.dto.response.FinalReportDto;
import com.holaho.intern.shared.dto.response.FinalReportSummaryDto;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.shared.mapper.EvaluationMapper;
import com.holaho.intern.shared.mapper.FinalReportMapper;
import com.holaho.intern.shared.mapper.WeeklyReportMapper;
import com.holaho.intern.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyReportServiceImpl implements WeeklyReportService {

    private final WeeklyReportRepository reportRepository;
    private final EvaluationRepository evaluationRepository;
    private final InternProfileRepository internRepository;
    private final UserRepository userRepository;
    private final MentorRepository mentorRepository;
    private final GroupMemberRepository groupMemberRepository;

    private final WeeklyReportMapper weeklyReportMapper;
    private final EvaluationMapper evaluationMapper;
    private final FinalReportMapper finalReportMapper;
    private final AiService aiService;

    @Override
    @Transactional(readOnly = true)
    public FinalReportDto getFinalReport(Long internId) {
        InternProfile intern = internRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern profile not found"));

        List<Evaluation> evaluations = evaluationRepository.findByInternId(internId);
        List<WeeklyReport> reports = reportRepository.findByIntern_IdOrderByWeekNumberDesc(internId);

        List<EvaluationResponse> evaluationDtos = evaluations.stream()
                .map(evaluationMapper::toResponse)
                .toList();

        List<WeeklyReportDto> reportDtos = reports.stream()
                .map(weeklyReportMapper::toDto)
                .toList();

        Double avgScore = evaluations.stream()
                .mapToInt(e -> e.getScore() != null ? e.getScore() : 0)
                .average()
                .orElse(0.0);

        String assessment = calculateAssessment(avgScore, evaluations.isEmpty());

        return finalReportMapper.toDto(
                intern,
                getMentorName(intern),
                "N/A", // Group name placeholder or fetch if needed
                evaluationDtos,
                reportDtos,
                Math.round(avgScore * 100.0) / 100.0,
                assessment,
                reports.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyReportDto> getByIntern(Long internId) {
        return reportRepository.findByIntern_IdOrderByWeekNumberDesc(internId).stream()
                .map(weeklyReportMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public WeeklyReportDto internSubmit(Long internId, WeeklyReportRequest req) {
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

        report.setStatus("SUBMITTED");
        report.setSubmittedAt(LocalDateTime.now());

        // Perform AI Sentiment Analysis
        try {
            String fullText = report.getCompletedWork() + " " + report.getChallenges();
            Map<String, Object> sentiment = aiService.analyzeSentiment(fullText);
            report.setSentimentLabel((String) sentiment.get("label"));
            report.setSentimentScore((Double) sentiment.get("score"));
        } catch (Exception e) {
            log.error("AI Sentiment Analysis failed for report", e);
        }

        report = reportRepository.save(report);
        log.info("Intern {} submitted report for week {}", internId, req.getWeekNumber());
        return weeklyReportMapper.toDto(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WeeklyReportDto> internMyReports(Long internId, Pageable pageable) {
        return reportRepository.findByIntern_Id(internId, pageable)
                .map(weeklyReportMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WeeklyReportDto> mentorGroupReports(Long mentorUserId, Long groupId, Long internId, String status,
            Pageable pageable) {
        Mentor mentor = mentorRepository.findByUser_Id(mentorUserId)
                .orElseThrow(() -> new NotFoundException("Mentor profile not found for user: " + mentorUserId));

        return reportRepository.findReportsForMentor(
                mentor.getId(),
                GroupStatus.ACTIVE,
                internId,
                status != null && !status.isEmpty() ? status : null,
                pageable)
                .map(weeklyReportMapper::toDto);
    }

    @Override
    @Transactional
    public WeeklyReportDto mentorReview(Long mentorUserId, Long reportId, ReviewWeeklyReportRequest req) {
        WeeklyReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found"));

        User mentor = userRepository.findById(mentorUserId)
                .orElseThrow(() -> new NotFoundException("Mentor user not found"));

        report.setMentor(mentor);
        report.setMentorFeedback(req.feedback());
        if (req.rating() != null) {
            report.setRating(req.rating());
        }
        report.setStatus("REVIEWED");
        report.setReviewedAt(LocalDateTime.now());

        report = reportRepository.save(report);
        return weeklyReportMapper.toDto(report);
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyReportDto getReportDetail(Long id) {
        WeeklyReport report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found"));
        return weeklyReportMapper.toDto(report);
    }

    @Override
    @Transactional
    public WeeklyReportDto updateStatus(Long id, String status) {
        WeeklyReport report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found"));
        report.setStatus(status);
        if ("REVIEWED".equals(status)) {
            report.setReviewedAt(LocalDateTime.now());
        }
        return weeklyReportMapper.toDto(reportRepository.save(report));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinalReportSummaryDto> getReportsSummary() {
        List<InternProfile> interns = internRepository.findAll();

        return interns.stream().map(intern -> {
            List<Evaluation> evaluations = evaluationRepository.findByInternId(intern.getId());
            long reportCount = reportRepository.countByIntern_Id(intern.getId());

            Double avgScore = evaluations.stream()
                    .mapToInt(e -> e.getScore() != null ? e.getScore() : 0)
                    .average()
                    .orElse(0.0);

            String assessment = calculateAssessment(avgScore, evaluations.isEmpty());

            return finalReportMapper.toSummaryDto(
                    intern,
                    getMentorName(intern),
                    Math.round(avgScore * 100.0) / 100.0,
                    assessment,
                    (int) reportCount);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternCountStatDto> getStatsByAssessment() {
        List<FinalReportSummaryDto> summaries = getReportsSummary();

        Map<String, Long> counts = summaries.stream()
                .collect(Collectors.groupingBy(
                        FinalReportSummaryDto::getFinalAssessment,
                        Collectors.counting()));

        return counts.entrySet().stream()
                .map(e -> new InternCountStatDto(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.count(), a.count()))
                .toList();
    }

    private String calculateAssessment(Double avgScore, boolean noEvaluations) {
        if (noEvaluations)
            return "Chưa đánh giá";
        if (avgScore >= 9.0)
            return "Xuất sắc";
        else if (avgScore >= 8.0)
            return "Giỏi";
        else if (avgScore >= 6.5)
            return "Khá";
        else if (avgScore >= 5.0)
            return "Trung bình";
        else
            return "Yếu";
    }

    private String getMentorName(InternProfile intern) {
        if (intern.getMentor() != null) {
            return intern.getMentor().getUser().getFullName();
        }
        return groupMemberRepository.findFirstByIntern_IdAndLeftAtIsNull(intern.getId())
                .map(gm -> gm.getGroup().getMentorId())
                .flatMap(mentorRepository::findById)
                .map(m -> m.getUser().getFullName())
                .orElse("N/A");
    }
}
