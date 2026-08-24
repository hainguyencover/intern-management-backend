package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractRevisionRequest {

    @NotBlank(message = "Lý do yêu cầu chỉnh sửa không được để trống")
    @Size(max = 2000, message = "Lý do tối đa 2000 ký tự")
    private String reason;
}
