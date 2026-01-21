package com.example.backend.service.impl;

import com.example.backend.dto.response.FinalEvaluationReportResponse;
import com.example.backend.dto.response.FinalEvaluationRowResponse;
import com.example.backend.entity.Evaluation;
import com.example.backend.entity.GroupMember;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Mentor;
import com.example.backend.repository.EvaluationRepository;
import com.example.backend.repository.GroupMemberRepository;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final EvaluationRepository evaluationRepository;
    private final InternProfileRepository internProfileRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public FinalEvaluationReportResponse generateFinalEvaluationReport(String period) {
        List<InternProfile> interns = internProfileRepository.findAllWithUserAndMentor();

        return buildReportFromInterns(interns, period);
    }

    @Override
    @Transactional(readOnly = true)
    public FinalEvaluationReportResponse generateFinalEvaluationReportByGroup(Long groupId, String period) {
        List<Long> internIds = groupMemberRepository.findInternIdsByGroupId(groupId);

        List<InternProfile> interns = internIds.isEmpty()
                ? List.of()
                : internProfileRepository.findByIdIn(internIds);

        return buildReportFromInterns(interns, period);
    }

    private FinalEvaluationReportResponse buildReportFromInterns(List<InternProfile> interns, String period) {
        // lấy internIds
        List<Long> internIds = interns.stream()
                .map(InternProfile::getId)
                .toList();

        // lấy evaluations theo list intern + period
        List<Evaluation> evaluations = internIds.isEmpty()
                ? List.of()
                : evaluationRepository.findByIntern_IdInAndPeriod(internIds, period);

        // map evaluation theo internId (nếu mỗi intern chỉ có 1 evaluation trong kỳ)
        Map<Long, Evaluation> evaluationByInternId = evaluations.stream()
                .collect(Collectors.toMap(
                        e -> e.getIntern().getId(),
                        e -> e,
                        (e1, e2) -> e1
                ));

        // build rows (intern nào thiếu evaluation vẫn có row)
        List<FinalEvaluationRowResponse> rows = interns.stream()
                .map(intern -> {
                    Evaluation ev = evaluationByInternId.get(intern.getId());

                    String internName = intern.getUser() != null ? intern.getUser().getFullName() : null;

                    Mentor mentor = intern.getMentor();
                    Long mentorId = mentor != null ? mentor.getId() : null;
                    String mentorName = null;
                    if (mentor != null && mentor.getUser() != null) {
                        mentorName = mentor.getUser().getFullName();
                    }

                    return FinalEvaluationRowResponse.builder()
                            .internId(intern.getId())
                            .studentCode(intern.getStudentCode())
                            .internName(internName)
                            .university(intern.getUniversity())
                            .major(intern.getMajor())
                            .gpa(intern.getGpa())

                            .mentorId(ev != null && ev.getMentor() != null ? ev.getMentor().getId() : mentorId)
                            .mentorName(ev != null && ev.getMentor() != null
                                    ? (ev.getMentor().getUser() != null ? ev.getMentor().getUser().getFullName() : null)
                                    : mentorName)

                            .skillScore(ev != null ? ev.getSkillScore() : null)
                            .attitudeScore(ev != null ? ev.getAttitudeScore() : null)
                            .overallScore(ev != null ? ev.getOverallScore() : null)
                            .comment(ev != null ? ev.getComment() : null)

                            .period(period)
                            .build();
                })
                .toList();

        // 5) tính summary average (chỉ tính trên intern có evaluation)
        Double avgSkill = averageInt(evaluations.stream().map(Evaluation::getSkillScore).toList());
        Double avgAttitude = averageInt(evaluations.stream().map(Evaluation::getAttitudeScore).toList());
        Double avgOverall = averageInt(evaluations.stream().map(Evaluation::getOverallScore).toList());

        return FinalEvaluationReportResponse.builder()
                .period(period)
                .totalInterns(interns.size())
                .totalEvaluations(evaluations.size())
                .avgSkillScore(avgSkill)
                .avgAttitudeScore(avgAttitude)
                .avgOverallScore(avgOverall)
                .rows(rows)
                .build();
    }

    private Double averageInt(List<Integer> values) {
        List<Integer> filtered = values.stream().filter(Objects::nonNull).toList();
        if (filtered.isEmpty()) return 0.0;
        double sum = filtered.stream().mapToDouble(Integer::doubleValue).sum();
        return sum / filtered.size();
    }

    // ===== Excel export =====
    @Override
    public byte[] exportFinalEvaluationReportExcel(FinalEvaluationReportResponse report) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Final Report");

            int rowIdx = 0;

            // Title
            Row title = sheet.createRow(rowIdx++);
            title.createCell(0).setCellValue("Final Evaluation Report - " + report.getPeriod());

            rowIdx++;

            // Summary
            Row summary1 = sheet.createRow(rowIdx++);
            summary1.createCell(0).setCellValue("Total interns");
            summary1.createCell(1).setCellValue(report.getTotalInterns());

            Row summary2 = sheet.createRow(rowIdx++);
            summary2.createCell(0).setCellValue("Total evaluations");
            summary2.createCell(1).setCellValue(report.getTotalEvaluations());

            Row summary3 = sheet.createRow(rowIdx++);
            summary3.createCell(0).setCellValue("Avg skill");
            summary3.createCell(1).setCellValue(report.getAvgSkillScore());

            Row summary4 = sheet.createRow(rowIdx++);
            summary4.createCell(0).setCellValue("Avg attitude");
            summary4.createCell(1).setCellValue(report.getAvgAttitudeScore());

            Row summary5 = sheet.createRow(rowIdx++);
            summary5.createCell(0).setCellValue("Avg overall");
            summary5.createCell(1).setCellValue(report.getAvgOverallScore());

            rowIdx += 2;

            // Header
            Row header = sheet.createRow(rowIdx++);
            String[] headers = {
                    "Intern ID", "Student Code", "Intern Name", "University", "Major", "GPA",
                    "Mentor", "Skill", "Attitude", "Overall", "Comment"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            // Rows
            for (FinalEvaluationRowResponse r : report.getRows()) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;

                row.createCell(col++).setCellValue(nvl(r.getInternId()));
                row.createCell(col++).setCellValue(nvl(r.getStudentCode()));
                row.createCell(col++).setCellValue(nvl(r.getInternName()));
                row.createCell(col++).setCellValue(nvl(r.getUniversity()));
                row.createCell(col++).setCellValue(nvl(r.getMajor()));
                row.createCell(col++).setCellValue(r.getGpa() != null ? r.getGpa() : 0);

                row.createCell(col++).setCellValue(nvl(r.getMentorName()));
                row.createCell(col++).setCellValue(r.getSkillScore() != null ? r.getSkillScore() : 0);
                row.createCell(col++).setCellValue(r.getAttitudeScore() != null ? r.getAttitudeScore() : 0);
                row.createCell(col++).setCellValue(r.getOverallScore() != null ? r.getOverallScore() : 0);
                row.createCell(col++).setCellValue(nvl(r.getComment()));
            }

            // autosize
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Export report excel failed", e);
        }
    }

    private String nvl(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }
}

