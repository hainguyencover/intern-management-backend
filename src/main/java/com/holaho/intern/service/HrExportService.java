package com.holaho.intern.service;

import com.holaho.intern.entity.Program;
import com.holaho.intern.service.HrProgramAnalyticsService;


import com.holaho.intern.shared.dto.InternCountStatDto;
import com.holaho.intern.shared.dto.ProgramCompletionStatDto;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HrExportService {

    private final HrAnalyticsService hrAnalyticsService;
    private final HrProgramAnalyticsService hrProgramAnalyticsService;

    public HrExportService(
            HrAnalyticsService hrAnalyticsService,
            HrProgramAnalyticsService hrProgramAnalyticsService) {
        this.hrAnalyticsService = hrAnalyticsService;
        this.hrProgramAnalyticsService = hrProgramAnalyticsService;
    }

    public byte[] exportInternSourceExcel(String groupBy) {
        List<InternCountStatDto> rows = hrAnalyticsService.countInterns(groupBy);

        try (Workbook wb = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Intern Source");

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numberStyle = createNumberStyle(wb);

            int r = 0;

            Row titleRow = sheet.createRow(r++);
            Cell t0 = titleRow.createCell(0);
            t0.setCellValue("Intern Source Report");
            t0.setCellStyle(headerStyle);

            Row metaRow = sheet.createRow(r++);
            metaRow.createCell(0).setCellValue("Group by: " + (groupBy == null ? "university" : groupBy));
            metaRow.createCell(1).setCellValue("Generated: " + now());

            r++;

            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("Key");
            header.createCell(1).setCellValue("Count");
            header.getCell(0).setCellStyle(headerStyle);
            header.getCell(1).setCellStyle(headerStyle);

            for (InternCountStatDto it : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(it.key());
                Cell c = row.createCell(1);
                c.setCellValue(it.count() == null ? 0 : it.count());
                c.setCellStyle(numberStyle);
            }

            autosize(sheet, 2);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Export Intern Source Excel failed", e);
        }
    }

    public byte[] exportInternSourcePdf(String groupBy) {
        List<InternCountStatDto> rows = hrAnalyticsService.countInterns(groupBy);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("Intern Source Report", pdfTitleFont()));
            doc.add(new Paragraph("Group by: " + (groupBy == null ? "university" : groupBy), pdfNormalFont()));
            doc.add(new Paragraph("Generated: " + now(), pdfNormalFont()));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 3f, 1f });

            table.addCell(pdfHeader("Key"));
            table.addCell(pdfHeader("Count"));

            for (InternCountStatDto it : rows) {
                table.addCell(pdfCell(it.key()));
                table.addCell(pdfCell(String.valueOf(it.count() == null ? 0 : it.count())));
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Export Intern Source PDF failed", e);
        }
    }

    public byte[] exportProgramCompletionExcel(String period, Long departmentId) {
        List<ProgramCompletionStatDto> rows = hrProgramAnalyticsService.completionRateByProgram(period, departmentId);

        try (Workbook wb = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Program Completion");

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle numberStyle = createNumberStyle(wb);
            CellStyle percentStyle = createPercentStyle(wb);

            int r = 0;

            Row titleRow = sheet.createRow(r++);
            Cell t0 = titleRow.createCell(0);
            t0.setCellValue("Program Completion Report");
            t0.setCellStyle(headerStyle);

            Row metaRow = sheet.createRow(r++);
            metaRow.createCell(0).setCellValue("Period: " + (period == null ? "FINAL" : period));
            metaRow.createCell(1).setCellValue("DepartmentId: " + (departmentId == null ? "ALL" : departmentId));
            metaRow.createCell(2).setCellValue("Generated: " + now());

            r++;

            Row header = sheet.createRow(r++);
            String[] cols = {
                    "Program ID", "Program Name",
                    "Total Interns", "Completed", "Completion Rate (%)"
            };

            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            for (ProgramCompletionStatDto x : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(x.programId());
                row.createCell(1).setCellValue(x.programName());

                Cell c2 = row.createCell(2);
                c2.setCellValue(x.totalInterns() == null ? 0 : x.totalInterns());
                c2.setCellStyle(numberStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(x.completedInterns() == null ? 0 : x.completedInterns());
                c3.setCellStyle(numberStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(x.completionRate() == null ? 0 : x.completionRate());
                c4.setCellStyle(percentStyle);
            }

            autosize(sheet, cols.length);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Export Program Completion Excel failed", e);
        }
    }

    public byte[] exportProgramCompletionPdf(String period, Long departmentId) {
        List<ProgramCompletionStatDto> rows = hrProgramAnalyticsService.completionRateByProgram(period, departmentId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            doc.add(new Paragraph("Program Completion Report", pdfTitleFont()));
            doc.add(new Paragraph("Period: " + (period == null ? "FINAL" : period), pdfNormalFont()));
            doc.add(new Paragraph("DepartmentId: " + (departmentId == null ? "ALL" : departmentId), pdfNormalFont()));
            doc.add(new Paragraph("Generated: " + now(), pdfNormalFont()));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.2f, 3.2f, 1.2f, 1.2f, 1.6f });

            table.addCell(pdfHeader("Program ID"));
            table.addCell(pdfHeader("Program Name"));
            table.addCell(pdfHeader("Total"));
            table.addCell(pdfHeader("Completed"));
            table.addCell(pdfHeader("Rate (%)"));

            for (ProgramCompletionStatDto x : rows) {
                table.addCell(pdfCell(String.valueOf(x.programId())));
                table.addCell(pdfCell(x.programName()));
                table.addCell(pdfCell(String.valueOf(x.totalInterns())));
                table.addCell(pdfCell(String.valueOf(x.completedInterns())));
                table.addCell(pdfCell(String.valueOf(x.completionRate())));
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Export Program Completion PDF failed", e);
        }
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private void autosize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++)
            sheet.autoSizeColumn(i);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        Font f = wb.createFont();
        f.setBold(true);
        CellStyle st = wb.createCellStyle();
        st.setFont(f);
        return st;
    }

    private CellStyle createNumberStyle(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        st.setDataFormat(wb.createDataFormat().getFormat("0"));
        return st;
    }

    private CellStyle createPercentStyle(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        st.setDataFormat(wb.createDataFormat().getFormat("0.00"));
        return st;
    }

    private com.lowagie.text.Font pdfTitleFont() {
        return new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA,
                16,
                com.lowagie.text.Font.BOLD);
    }

    private com.lowagie.text.Font pdfNormalFont() {
        return new com.lowagie.text.Font(
                com.lowagie.text.Font.HELVETICA,
                11,
                com.lowagie.text.Font.NORMAL);
    }

    private PdfPCell pdfHeader(String s) {
        PdfPCell c = new PdfPCell(
                new Phrase(
                        s,
                        new com.lowagie.text.Font(
                                com.lowagie.text.Font.HELVETICA,
                                11,
                                com.lowagie.text.Font.BOLD)));
        c.setPadding(6);
        return c;
    }

    private PdfPCell pdfCell(String s) {
        PdfPCell c = new PdfPCell(
                new Phrase(
                        s == null ? "" : s,
                        new com.lowagie.text.Font(
                                com.lowagie.text.Font.HELVETICA,
                                11,
                                com.lowagie.text.Font.NORMAL)));
        c.setPadding(6);
        return c;
    }
}

