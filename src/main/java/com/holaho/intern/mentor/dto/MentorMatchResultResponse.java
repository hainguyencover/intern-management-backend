package com.holaho.intern.mentor.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorMatchResultResponse {
    private MentorProfileDetailResponse mentor;
    private Integer matchPercentage;
    private Double skillMatchScore;
    private Double experienceMatchScore;
    private Double domainMatchScore;
    private Double capacityMatchScore;
    private List<String> matchedSkills;
    private List<String> matchedDomains;
    private String matchReason;
}
