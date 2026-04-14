package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
    private String fullName;

    private String phone;

    private String address;
}

