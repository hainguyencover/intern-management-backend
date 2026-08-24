package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityCheckResponse {
    private boolean eligible;
    private List<RuleCheck> checks;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleCheck {
        private String rule;
        private boolean passed;
        private String message;
    }
}
