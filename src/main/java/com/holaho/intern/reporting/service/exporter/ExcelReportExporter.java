package com.holaho.intern.reporting.service.exporter;

import com.holaho.intern.reporting.dto.ReportData;
import com.holaho.intern.reporting.enums.ExportFormat;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class ExcelReportExporter implements ReportExporter {

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.XLSX;
    }

    @Override
    public byte[] export(ReportData data) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(data.getReportName() != null ? data.getReportName() : "Report");
            sheet.setDisplayGridlines(true);

            // Styles
            Font titleFont = workbook.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);

            Font subFont = workbook.createFont();
            subFont.setFontName("Calibri");
            subFont.setFontHeightInPoints((short) 10);
            subFont.setItalic(true);
            subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            CellStyle subStyle = workbook.createCellStyle();
            subStyle.setFont(subFont);

            Font summaryLabelFont = workbook.createFont();
            summaryLabelFont.setFontName("Calibri");
            summaryLabelFont.setBold(true);

            CellStyle summaryLabelStyle = workbook.createCellStyle();
            summaryLabelStyle.setFont(summaryLabelFont);

            Font headerFont = workbook.createFont();
            headerFont.setFontName("Calibri");
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.NAVY.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            int rowIdx = 0;

            // Header Banner
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("IMS - " + data.getReportName().toUpperCase());
            titleCell.setCellStyle(titleStyle);

            Row metaRow = sheet.createRow(rowIdx++);
            Cell metaCell = metaRow.createCell(0);
            String timeStr = data.getGeneratedAt() != null ? data.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            metaCell.setCellValue("Thời gian xuất: " + timeStr + " | Người xuất: " + (data.getGeneratedBy() != null ? data.getGeneratedBy() : "Hệ thống HR"));
            metaCell.setCellStyle(subStyle);

            rowIdx++; // Blank space

            // Summary Section
            if (data.getSummary() != null && !data.getSummary().isEmpty()) {
                Row sumHeaderRow = sheet.createRow(rowIdx++);
                Cell sumHeaderCell = sumHeaderRow.createCell(0);
                sumHeaderCell.setCellValue("TỔNG QUAN BÁO CÁO");
                sumHeaderCell.setCellStyle(summaryLabelStyle);

                for (Map.Entry<String, Object> entry : data.getSummary().entrySet()) {
                    Row sumRow = sheet.createRow(rowIdx++);
                    Cell lblCell = sumRow.createCell(0);
                    lblCell.setCellValue(entry.getKey() + ":");
                    lblCell.setCellStyle(summaryLabelStyle);

                    Cell valCell = sumRow.createCell(1);
                    valCell.setCellValue(String.valueOf(entry.getValue()));
                }
                rowIdx++; // Blank space
            }

            // Table Headers
            List<String> columns = data.getColumns();
            Row tableHeaderRow = sheet.createRow(rowIdx++);
            tableHeaderRow.setHeightInPoints(24);
            int dataHeaderRowIdx = rowIdx;

            for (int col = 0; col < columns.size(); col++) {
                Cell cell = tableHeaderRow.createCell(col);
                cell.setCellValue(columns.get(col));
                cell.setCellStyle(headerStyle);
            }

            // Table Rows
            List<Map<String, Object>> rows = data.getRows();
            if (rows != null) {
                for (Map<String, Object> rowMap : rows) {
                    Row r = sheet.createRow(rowIdx++);
                    for (int col = 0; col < columns.size(); col++) {
                        Cell c = r.createCell(col);
                        c.setCellStyle(dataStyle);

                        String colName = columns.get(col);
                        Object val = rowMap.get(colName);
                        if (val != null) {
                            if (val instanceof Number num) {
                                c.setCellValue(num.doubleValue());
                            } else {
                                c.setCellValue(String.valueOf(val));
                            }
                        } else {
                            c.setCellValue("");
                        }
                    }
                }
            }

            // Freeze Pane on Data Header
            sheet.createFreezePane(0, dataHeaderRowIdx);

            // Auto-size columns
            for (int col = 0; col < columns.size(); col++) {
                sheet.autoSizeColumn(col);
                int currentWidth = sheet.getColumnWidth(col);
                sheet.setColumnWidth(col, Math.max(currentWidth + 1000, 3500));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tạo file Excel báo cáo: " + e.getMessage(), e);
        }
    }
}
