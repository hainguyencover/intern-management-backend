package com.holaho.intern.mentor.dto;

import com.holaho.intern.shared.dto.response.MentorResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record MentorSuggestionResponse(
        Long internId,
        String internName,
        String internDepartmentName,
        List<SuggestedMentorItem> suggestions
) {
    @Builder
    public record SuggestedMentorItem(
            MentorResponse mentor,
            int matchScorePercentage,
            String matchReason,
            boolean sameDepartmentMatch,
            int currentAssignedCount,
            int maxCapacity
    ) {}
}
