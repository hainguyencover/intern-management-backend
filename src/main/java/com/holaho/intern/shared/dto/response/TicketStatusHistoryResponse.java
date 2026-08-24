package com.holaho.intern.shared.dto.response;

import com.holaho.intern.entity.TicketStatusHistory;
import com.holaho.intern.shared.enums.TicketStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatusHistoryResponse {
    private Long id;
    private TicketStatus fromStatus;
    private TicketStatus toStatus;
    private String changedByName;
    private String reason;
    private LocalDateTime createdAt;

    public static TicketStatusHistoryResponse from(TicketStatusHistory history) {
        if (history == null) return null;
        return TicketStatusHistoryResponse.builder()
                .id(history.getId())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .changedByName(history.getChangedBy() != null ? history.getChangedBy().getFullName() : null)
                .reason(history.getReason())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
