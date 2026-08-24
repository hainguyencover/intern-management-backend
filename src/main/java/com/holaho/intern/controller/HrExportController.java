package com.holaho.intern.controller;

import com.holaho.intern.service.HrExportService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/hr/exports")
public class HrExportController {

    private final HrExportService hrExportService;

    public HrExportController(HrExportService hrExportService) {
        this.hrExportService = hrExportService;
    }

    /**
     * report:
     * - intern_source (params: groupBy=university|major|university_major)
     * - program_completion (params: period=FINAL, departmentId?)
     *
     * format: xlsx | pdf
     */
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    @GetMapping
    public ResponseEntity<byte[]> export(
            @RequestParam String report,
            @RequestParam String format,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long departmentId) {
        String r = report.trim().toLowerCase();
        String f = format.trim().toLowerCase();

        byte[] bytes;
        String filename;
        MediaType contentType;

        if ("intern_source".equals(r)) {
            if ("xlsx".equals(f)) {
                bytes = hrExportService.exportInternSourceExcel(groupBy);
                filename = "intern_source.xlsx";
                contentType = MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else if ("pdf".equals(f)) {
                bytes = hrExportService.exportInternSourcePdf(groupBy);
                filename = "intern_source.pdf";
                contentType = MediaType.APPLICATION_PDF;
            } else {
                return ResponseEntity.badRequest().body(("Invalid format: " + format).getBytes(StandardCharsets.UTF_8));
            }
        } else if ("program_completion".equals(r)) {
            if ("xlsx".equals(f)) {
                bytes = hrExportService.exportProgramCompletionExcel(period, departmentId);
                filename = "program_completion.xlsx";
                contentType = MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else if ("pdf".equals(f)) {
                bytes = hrExportService.exportProgramCompletionPdf(period, departmentId);
                filename = "program_completion.pdf";
                contentType = MediaType.APPLICATION_PDF;
            } else {
                return ResponseEntity.badRequest().body(("Invalid format: " + format).getBytes(StandardCharsets.UTF_8));
            }
        } else {
            return ResponseEntity.badRequest().body(("Invalid report: " + report).getBytes(StandardCharsets.UTF_8));
        }

        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .cacheControl(CacheControl.noCache())
                .body(bytes);
    }
}

