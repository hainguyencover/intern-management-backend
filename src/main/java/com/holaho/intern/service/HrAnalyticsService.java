package com.holaho.intern.service;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.intern.repository.specification.AnalyticsSpecification;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.dto.InternCountStatDto;
import com.holaho.intern.shared.dto.request.AnalyticsFilterRequest;
import com.holaho.intern.shared.dto.response.*;
import com.holaho.intern.shared.exception.BadRequestException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HrAnalyticsService {

    private final InternProfileRepository internProfileRepository;

    public HrAnalyticsService(InternProfileRepository internProfileRepository) {
        this.internProfileRepository = internProfileRepository;
    }

    public AnalyticsOverviewResponse getOverview(AnalyticsFilterRequest filter) {
        validateFilter(filter);
        Long tenantId = TenantContext.getCurrentTenantId();

        Specification<InternProfile> spec = AnalyticsSpecification.buildSpecification(filter, tenantId);
        List<InternProfile> profiles = internProfileRepository.findAll(spec);

        long totalInterns = profiles.size();
        long interningInterns = profiles.stream()
                .filter(p -> "INTERNING".equalsIgnoreCase(p.getStatus()) || "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .count();
        long completedInterns = profiles.stream()
                .filter(p -> "COMPLETED".equalsIgnoreCase(p.getStatus()))
                .count();
        long participatingInterns = interningInterns + completedInterns;

        double completionRate = participatingInterns == 0 ? 0.0
                : Math.round((completedInterns * 100.0 / participatingInterns) * 100.0) / 100.0;

        return AnalyticsOverviewResponse.builder()
                .totalInterns(totalInterns)
                .participatingInterns(participatingInterns)
                .interningInterns(interningInterns)
                .completedInterns(completedInterns)
                .completionRate(completionRate)
                .build();
    }

    public List<SchoolStatisticResponse> getBySchool(AnalyticsFilterRequest filter) {
        validateFilter(filter);
        Long tenantId = TenantContext.getCurrentTenantId();

        Specification<InternProfile> spec = AnalyticsSpecification.buildSpecification(filter, tenantId);
        List<InternProfile> profiles = internProfileRepository.findAll(spec);

        long totalCount = profiles.size();
        if (totalCount == 0) {
            return Collections.emptyList();
        }

        Map<String, Long> countsBySchool = profiles.stream()
                .collect(Collectors.groupingBy(
                        p -> (p.getUniversity() != null && !p.getUniversity().isBlank()) ? p.getUniversity().trim() : "Khác / Chưa cập nhật",
                        Collectors.counting()
                ));

        return countsBySchool.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    long count = entry.getValue();
                    double pct = Math.round((count * 100.0 / totalCount) * 100.0) / 100.0;
                    return SchoolStatisticResponse.builder()
                            .schoolId(null)
                            .schoolName(entry.getKey())
                            .count(count)
                            .percentage(pct)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<MajorStatisticResponse> getByMajor(AnalyticsFilterRequest filter) {
        validateFilter(filter);
        Long tenantId = TenantContext.getCurrentTenantId();

        Specification<InternProfile> spec = AnalyticsSpecification.buildSpecification(filter, tenantId);
        List<InternProfile> profiles = internProfileRepository.findAll(spec);

        long totalCount = profiles.size();
        if (totalCount == 0) {
            return Collections.emptyList();
        }

        Map<String, Long> countsByMajor = profiles.stream()
                .collect(Collectors.groupingBy(
                        p -> (p.getMajor() != null && !p.getMajor().isBlank()) ? p.getMajor().trim() : "Khác / Chưa cập nhật",
                        Collectors.counting()
                ));

        return countsByMajor.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    long count = entry.getValue();
                    double pct = Math.round((count * 100.0 / totalCount) * 100.0) / 100.0;
                    return MajorStatisticResponse.builder()
                            .majorId(null)
                            .majorName(entry.getKey())
                            .count(count)
                            .percentage(pct)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public CompletionStatisticResponse getCompletion(AnalyticsFilterRequest filter) {
        validateFilter(filter);
        Long tenantId = TenantContext.getCurrentTenantId();

        Specification<InternProfile> spec = AnalyticsSpecification.buildSpecification(filter, tenantId);
        List<InternProfile> profiles = internProfileRepository.findAll(spec);

        long interning = profiles.stream()
                .filter(p -> "INTERNING".equalsIgnoreCase(p.getStatus()) || "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .count();
        long completed = profiles.stream()
                .filter(p -> "COMPLETED".equalsIgnoreCase(p.getStatus()))
                .count();
        long totalParticipating = interning + completed;

        double rate = totalParticipating == 0 ? 0.0
                : Math.round((completed * 100.0 / totalParticipating) * 100.0) / 100.0;

        return CompletionStatisticResponse.builder()
                .totalParticipating(totalParticipating)
                .completed(completed)
                .interning(interning)
                .completionRate(rate)
                .build();
    }

    public List<InternCountStatDto> countInterns(String groupBy) {
        String gb = (groupBy == null || groupBy.isBlank()) ? "university" : groupBy.trim().toLowerCase();

        return switch (gb) {
            case "university" -> internProfileRepository.countInternsGroupedByUniversity();
            case "major" -> internProfileRepository.countInternsGroupedByMajor();
            case "university_major" -> internProfileRepository.countByUniversityAndMajor();
            default -> throw new BadRequestException("groupBy must be: university | major | university_major");
        };
    }

    private void validateFilter(AnalyticsFilterRequest filter) {
        if (filter != null && filter.getFromDate() != null && filter.getToDate() != null) {
            if (filter.getFromDate().isAfter(filter.getToDate())) {
                throw new BadRequestException("fromDate must be before or equal to toDate");
            }
        }
    }
}
