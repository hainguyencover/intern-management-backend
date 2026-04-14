package com.holaho.intern.shared.dto.response;

import com.holaho.intern.entity.Allowance;
import com.holaho.intern.shared.enums.AllowanceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AllowanceResponse {
    private Long id;
    private Long internId;
    private String internName;
    private BigDecimal amount;
    private LocalDate allowanceMonth;
    private LocalDate paymentDate;
    private AllowanceStatus status;
    private String notes;
    private String paidByName;

    public static AllowanceResponse from(Allowance allowance) {
        return AllowanceResponse.builder()
                .id(allowance.getId())
                .internId(allowance.getIntern().getId())
                .internName(allowance.getIntern().getUser().getFullName())
                .amount(allowance.getAmount())
                .allowanceMonth(allowance.getAllowanceMonth())
                .paymentDate(allowance.getPaymentDate())
                .status(allowance.getStatus())
                .notes(allowance.getNotes())
                .paidByName(allowance.getPaidBy() != null ?
                        allowance.getPaidBy().getFullName() : null)
                .build();
    }
}

