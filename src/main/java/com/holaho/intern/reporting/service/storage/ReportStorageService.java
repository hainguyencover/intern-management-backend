package com.holaho.intern.reporting.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class ReportStorageService {

    private final Path storageDirectory;

    public ReportStorageService(@Value("${app.reporting.storage-dir:uploads/reports}") String storageDirStr) {
        this.storageDirectory = Paths.get(storageDirStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            log.error("Failed to create report storage directory: {}", storageDirStr, e);
        }
    }

    public String storeFile(String fileName, byte[] data) throws IOException {
        Files.createDirectories(this.storageDirectory);
        Path targetPath = this.storageDirectory.resolve(fileName);
        Files.write(targetPath, data);
        log.info("Report file saved successfully to: {}", targetPath);
        return targetPath.toString();
    }

    public byte[] loadFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File báo cáo không tồn tại hoặc đã bị xóa: " + filePath);
        }
        return Files.readAllBytes(path);
    }

    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) return false;
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Lỗi khi xóa file báo cáo hết hạn: {}", filePath, e);
            return false;
        }
    }
}
