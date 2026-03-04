package com.example.backend.mapper;

import com.example.backend.dto.WeeklyReportDto;
import com.example.backend.dto.response.EvaluationResponse;
import com.example.backend.dto.response.FinalReportDto;
import com.example.backend.dto.response.FinalReportSummaryDto;
import com.example.backend.entity.InternProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FinalReportMapper {

    @Mapping(target = "fullName", source = "intern.user.fullName")
    @Mapping(target = "email", source = "intern.user.email")
    @Mapping(target = "internId", source = "intern.id")
    @Mapping(target = "studentCode", source = "intern.studentCode")
    @Mapping(target = "university", source = "intern.university")
    @Mapping(target = "major", source = "intern.major")
    @Mapping(target = "startDate", source = "intern.startDate")
    @Mapping(target = "endDate", source = "intern.endDate")
    @Mapping(target = "mentorName", source = "mentorName")
    @Mapping(target = "groupName", source = "groupName")
    @Mapping(target = "evaluations", source = "evaluations")
    @Mapping(target = "weeklyReports", source = "weeklyReports")
    @Mapping(target = "finalScore", source = "finalScore")
    @Mapping(target = "finalAssessment", source = "finalAssessment")
    @Mapping(target = "totalReports", source = "totalReports")
    FinalReportDto toDto(InternProfile intern,
            String mentorName,
            String groupName,
            List<EvaluationResponse> evaluations,
            List<WeeklyReportDto> weeklyReports,
            Double finalScore,
            String finalAssessment,
            Integer totalReports);

    @Mapping(target = "internId", source = "intern.id")
    @Mapping(target = "fullName", source = "intern.user.fullName")
    @Mapping(target = "studentCode", source = "intern.studentCode")
    @Mapping(target = "university", source = "intern.university")
    @Mapping(target = "mentorName", source = "mentorName")
    @Mapping(target = "finalScore", source = "finalScore")
    @Mapping(target = "finalAssessment", source = "finalAssessment")
    @Mapping(target = "reportCount", source = "reportCount")
    FinalReportSummaryDto toSummaryDto(InternProfile intern,
            String mentorName,
            Double finalScore,
            String finalAssessment,
            Integer reportCount);
}
