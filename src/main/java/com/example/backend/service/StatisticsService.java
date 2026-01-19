package com.example.backend.service;

import com.example.backend.dto.InternCountStatDto;
import com.example.backend.dto.response.DashboardOverviewResponse;
import com.example.backend.enums.ApplicationStatus;
import com.example.backend.enums.GroupStatus;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;
    private final ProgramRepository programRepository;
    private final ProgramGroupRepository groupRepository;
    private final ApplicationRepository applicationRepository;
    private final InternDocumentRepository internDocumentRepository;

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview() {
        return DashboardOverviewResponse.builder()
                .totalInterns(internProfileRepository.count())
                .totalMentors(mentorRepository.count())
                .totalPrograms(programRepository.count())
                .activeGroups(groupRepository.countByStatus(GroupStatus.ACTIVE))
                .pendingApplications(applicationRepository.countByStatus(ApplicationStatus.SUBMITTED))
                .documentsToReview(internDocumentRepository.countByStatus("PENDING"))
                .activePrograms(programRepository.countByStatus(com.example.backend.enums.ProgramStatus.ACTIVE))
                .completionRate(calculateCompletionRate())
                .build();
    }

    private Double calculateCompletionRate() {
        long totalInterns = internProfileRepository.count();
        if (totalInterns == 0)
            return 0.0;

        long completedInterns = internProfileRepository.findAll().stream()
                .filter(i -> i.getEndDate() != null && i.getEndDate().isBefore(java.time.LocalDate.now()))
                .count();

        return ((double) completedInterns / totalInterns) * 100.0;
    }

    @Transactional(readOnly = true)
    public List<InternCountStatDto> getUniversityStats() {
        return internProfileRepository.countByUniversityAndMajor();
    }
}
