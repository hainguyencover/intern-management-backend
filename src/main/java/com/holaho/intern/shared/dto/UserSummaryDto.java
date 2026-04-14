package com.holaho.intern.shared.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UserSummaryDto {
    private Long id;
    private String fullName;
    private String email;
}

