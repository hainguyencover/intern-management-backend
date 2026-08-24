package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewQueueStatsDto {
    private long pendingReviewCount;
    private long inReviewCount;
    private long overdueCount;
    private long approvedTodayCount;
    private long rejectedTodayCount;
    private long needsRevisionCount;
}
