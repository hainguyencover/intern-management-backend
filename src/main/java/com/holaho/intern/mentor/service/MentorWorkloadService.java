package com.holaho.intern.mentor.service;

import com.holaho.intern.mentor.dto.MentorActiveInternResponse;
import com.holaho.intern.mentor.dto.MentorWorkloadResponse;
import com.holaho.intern.mentor.dto.MentorWorkloadSummaryResponse;
import com.holaho.intern.shared.enums.MentorWorkloadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MentorWorkloadService {

    Page<MentorWorkloadResponse> getWorkloads(
            String keyword,
            Long departmentId,
            MentorWorkloadStatus workloadStatus,
            Pageable pageable
    );

    MentorWorkloadSummaryResponse getWorkloadSummary();

    List<MentorActiveInternResponse> getMentorActiveInterns(Long mentorId);

    MentorWorkloadStatus calculateWorkloadStatus(int currentCount, int maxCapacity);
}
