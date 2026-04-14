package com.holaho.intern.service;

import com.holaho.intern.shared.dto.InternCountStatDto;
import com.holaho.intern.repository.InternProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HrAnalyticsService {

    private final InternProfileRepository internProfileRepository;

    public HrAnalyticsService(InternProfileRepository internProfileRepository) {
        this.internProfileRepository = internProfileRepository;
    }

    public List<InternCountStatDto> countInterns(String groupBy) {
        String gb = (groupBy == null || groupBy.isBlank()) ? "university" : groupBy.trim().toLowerCase();

        return switch (gb) {
            case "university" -> internProfileRepository.countInternsGroupedByUniversity();
            case "major" -> internProfileRepository.countInternsGroupedByMajor();
            case "university_major" -> internProfileRepository.countByUniversityAndMajor();
            default -> throw new IllegalArgumentException("groupBy must be: university | major | university_major");
        };
    }
}

