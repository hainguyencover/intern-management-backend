package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProgramUpdateRequest {

    // optional: cho phép đổi department (nếu bạn muốn khóa thì bỏ field này)
    private Long departmentId;

    @NotBlank(message = "name must not be blank")
    private String name;

    private String description;

    private LocalDate startDate;
    private LocalDate endDate;
}
