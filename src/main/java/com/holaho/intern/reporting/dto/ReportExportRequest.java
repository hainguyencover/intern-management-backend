package com.holaho.intern.reporting.dto;

import com.holaho.intern.reporting.enums.ExportFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportExportRequest {

    @NotBlank(message = "mã báo cáo không được để trống")
    private String reportCode;

    @NotNull(message = "định dạng xuất không được để trống")
    private ExportFormat format;

    private ReportFilterRequest filters;
}
