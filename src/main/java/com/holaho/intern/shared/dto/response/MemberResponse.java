package com.holaho.intern.shared.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MemberResponse {
    private Long internId;
    private String internName;
    private LocalDateTime joinedAt;
}

