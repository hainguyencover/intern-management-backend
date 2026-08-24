package com.holaho.intern.reporting.dto;

import com.holaho.intern.reporting.enums.ExportFormat;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCatalogDto {
    private String code;
    private String name;
    private String description;
    private String category;
    private List<ExportFormat> supportedFormats;
    private List<String> availableFilters;
}
