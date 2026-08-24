package com.holaho.intern.mentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorCertificationRequest {

    @NotBlank(message = "Tên chứng chỉ không được để trống")
    @Size(max = 200, message = "Tên chứng chỉ không vượt quá 200 ký tự")
    private String name;

    @Size(max = 200, message = "Tổ chức cấp không vượt quá 200 ký tự")
    private String issuingOrganization;

    @Size(max = 150, message = "Mã chứng chỉ không vượt quá 150 ký tự")
    private String credentialId;

    private LocalDate issuedDate;

    private LocalDate expiryDate;

    @Size(max = 500, message = "URL chứng chỉ không vượt quá 500 ký tự")
    private String credentialUrl;
}
