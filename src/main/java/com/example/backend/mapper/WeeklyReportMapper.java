package com.example.backend.mapper;

import com.example.backend.dto.WeeklyReportDto;
import com.example.backend.entity.WeeklyReport;
import com.example.backend.enums.WeeklyReportStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WeeklyReportMapper {

    @Mapping(target = "internId", source = "intern.id")
    @Mapping(target = "internName", source = "intern.user.fullName")
    @Mapping(target = "status", source = "status", qualifiedByName = "toReportStatus")
    @Mapping(target = "mentorId", source = "mentor.id")
    @Mapping(target = "mentorName", source = "mentor.fullName")
    @Mapping(target = "summary", source = "learnings") // Alias from MentorService logic
    @Mapping(target = "sentimentLabel", source = "sentimentLabel")
    @Mapping(target = "sentimentScore", source = "sentimentScore")
    WeeklyReportDto toDto(WeeklyReport report);

    @Named("toReportStatus")
    default WeeklyReportStatus toReportStatus(String status) {
        if (status == null)
            return null;
        try {
            return WeeklyReportStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
