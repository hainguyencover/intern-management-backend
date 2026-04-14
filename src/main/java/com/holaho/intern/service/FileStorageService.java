package com.holaho.intern.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Store file and return relative path
     * 
     * @param file   MultipartFile to store
     * @param subDir subdirectory (e.g. "contracts", "documents")
     * @return relative file path
     */
    public String store(MultipartFile file, String subDir) {
        try {
            // Create directory if not exists
            Path uploadPath = Paths.get(uploadDir, subDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored: {}", filePath);

            // Return relative path
            return subDir + "/" + filename;

        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new RuntimeException("KhÃ´ng thá»ƒ lÆ°u file: " + e.getMessage());
        }
    }

    /**
     * Delete file by relative path
     */
    public void delete(String relativePath) {
        try {
            Path filePath = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", relativePath, e);
        }
    }

    /**
     * Get absolute path for a relative path
     */
    public Path getPath(String relativePath) {
        return Paths.get(uploadDir, relativePath);
    }
}

