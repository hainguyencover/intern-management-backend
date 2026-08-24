package com.holaho.intern.mentor.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorCertificationResponse {
    private Long id;
    private String name;
    private String issuingOrganization;
    private String credentialId;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private String credentialUrl;
}
