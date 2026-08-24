package com.holaho.intern.reporting.enums;

public enum ExportFormat {
    XLSX,
    PDF;

    public String getContentType() {
        return switch (this) {
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case PDF -> "application/pdf";
        };
    }

    public String getFileExtension() {
        return switch (this) {
            case XLSX -> ".xlsx";
            case PDF -> ".pdf";
        };
    }
}
