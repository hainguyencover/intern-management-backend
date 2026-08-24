package com.holaho.intern.mentor.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentoringDomainResponse {
    private Long id;
    private String name;
    private String description;
    private Boolean isActive;
}
