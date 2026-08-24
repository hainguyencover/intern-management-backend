package com.holaho.intern.reporting.service.exporter;

import com.holaho.intern.reporting.dto.ReportData;
import com.holaho.intern.reporting.enums.ExportFormat;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class PdfReportExporter implements ReportExporter {

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.PDF;
    }

    @Override
    public byte[] export(ReportData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 54, 54);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Page Event for Header & Footer
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document doc) {
                    PdfContentByte cb = writer.getDirectContent();

                    Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

                    // Header Line
                    cb.setColorStroke(new Color(200, 200, 200));
                    cb.moveTo(36, doc.getPageSize().getHeight() - 36);
                    cb.lineTo(doc.getPageSize().getWidth() - 36, doc.getPageSize().getHeight() - 36);
                    cb.stroke();

                    // Footer Line & Page Number
                    cb.moveTo(36, 36);
                    cb.lineTo(doc.getPageSize().getWidth() - 36, 36);
                    cb.stroke();

                    String pageText = "Trang " + writer.getPageNumber();
                    ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(pageText, footerFont), doc.getPageSize().getWidth() - 36, 24, 0);
                    ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase("BÁO CÁO HỆ THỐNG QUẢN LÝ THỰC TẬP SINH (IMS)", footerFont), 36, 24, 0);
                }
            });

            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(27, 54, 93));
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(27, 54, 93));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

            // Title
            Paragraph title = new Paragraph(data.getReportName() != null ? data.getReportName().toUpperCase() : "BÁO CÁO", titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            title.setSpacingAfter(4);
            document.add(title);

            // Metadata
            String timeStr = data.getGeneratedAt() != null ? data.getGeneratedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            Paragraph meta = new Paragraph("Ngày xuất: " + timeStr + " | Đơn vị: IMS Corporate | Người xuất: " + (data.getGeneratedBy() != null ? data.getGeneratedBy() : "HR Admin"), metaFont);
            meta.setSpacingAfter(15);
            document.add(meta);

            // Summary Section
            if (data.getSummary() != null && !data.getSummary().isEmpty()) {
                Paragraph sumTitle = new Paragraph("TỔNG QUAN THỐNG KÊ", sectionFont);
                sumTitle.setSpacingAfter(8);
                document.add(sumTitle);

                PdfPTable sumTable = new PdfPTable(2);
                sumTable.setWidthPercentage(50);
                sumTable.setHorizontalAlignment(Element.ALIGN_LEFT);
                sumTable.setSpacingAfter(15);

                for (Map.Entry<String, Object> entry : data.getSummary().entrySet()) {
                    PdfPCell keyCell = new PdfPCell(new Phrase(entry.getKey(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
                    keyCell.setBackgroundColor(new Color(245, 247, 250));
                    keyCell.setPadding(5);

                    PdfPCell valCell = new PdfPCell(new Phrase(String.valueOf(entry.getValue()), cellFont));
                    valCell.setPadding(5);

                    sumTable.addCell(keyCell);
                    sumTable.addCell(valCell);
                }
                document.add(sumTable);
            }

            // Data Table
            List<String> columns = data.getColumns();
            List<Map<String, Object>> rows = data.getRows();

            if (columns != null && !columns.isEmpty()) {
                PdfPTable table = new PdfPTable(columns.size());
                table.setWidthPercentage(100);
                table.setSpacingBefore(10);
                table.setHeaderRows(1);

                // Table Header
                for (String colName : columns) {
                    PdfPCell headerCell = new PdfPCell(new Phrase(colName, headerFont));
                    headerCell.setBackgroundColor(new Color(27, 54, 93));
                    headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    headerCell.setPadding(6);
                    table.addCell(headerCell);
                }

                // Table Rows
                if (rows != null) {
                    boolean alternate = false;
                    for (Map<String, Object> rowMap : rows) {
                        Color rowBg = alternate ? new Color(248, 249, 250) : Color.WHITE;
                        alternate = !alternate;

                        for (String colName : columns) {
                            Object val = rowMap.get(colName);
                            PdfPCell cell = new PdfPCell(new Phrase(val != null ? String.valueOf(val) : "", cellFont));
                            cell.setBackgroundColor(rowBg);
                            cell.setPadding(5);
                            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                            table.addCell(cell);
                        }
                    }
                }

                document.add(table);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xuất PDF báo cáo: " + e.getMessage(), e);
        }
    }
}
