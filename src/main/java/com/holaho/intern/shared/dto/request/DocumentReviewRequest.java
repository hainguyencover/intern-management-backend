package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentReviewRequest(
        @NotBlank(message = "Lý do không được để trống")
        @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
        String reason,
        
        String note
) {
        public String getEffectiveReason() {
                if (reason != null && !reason.isBlank()) {
                        return reason.trim();
                }
                if (note != null && !note.isBlank()) {
                        return note.trim();
                }
                return "";
        }
}
