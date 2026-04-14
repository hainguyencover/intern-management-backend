package com.holaho.intern.shared.dto.request;

import com.holaho.intern.shared.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {
    @NotNull(message = "Status is required")
    private UserStatus status;
}

