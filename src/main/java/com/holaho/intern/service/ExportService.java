package com.holaho.intern.service;

import com.holaho.intern.entity.InternProfile;
import com.holaho.intern.entity.Task;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    public byte[] exportInternsToExcel(List<InternProfile> interns) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Interns");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Full Name");
            header.createCell(1).setCellValue("Email");
            header.createCell(2).setCellValue("University");
            header.createCell(3).setCellValue("Major");
            header.createCell(4).setCellValue("GPA");

            int rowIdx = 1;
            for (InternProfile intern : interns) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(intern.getUser().getFullName());
                row.createCell(1).setCellValue(intern.getUser().getEmail());
                row.createCell(2).setCellValue(intern.getUniversity());
                row.createCell(3).setCellValue(intern.getMajor());
                row.createCell(4).setCellValue(intern.getGpa() != null ? intern.getGpa() : 0.0);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportTasksToPdf(List<Task> tasks) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("Task List Report"));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.addCell("Title");
        table.addCell("Status");
        table.addCell("Assignee");
        table.addCell("Due Date");

        for (Task task : tasks) {
            table.addCell(task.getTitle());
            table.addCell(task.getStatus().name());
            table.addCell(task.getAssignee() != null ? task.getAssignee().getUser().getFullName() : "Unassigned");
            table.addCell(task.getDueDate() != null ? task.getDueDate().toString() : "N/A");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }
}

