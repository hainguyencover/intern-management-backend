package com.holaho.intern.service;

import com.holaho.intern.shared.dto.StoredFile;
import com.holaho.intern.shared.enums.DocumentType;
import com.holaho.intern.shared.exception.FileStorageException;
import com.holaho.intern.shared.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootDir;

    public LocalStorageService(@Value("${app.storage.local-root:uploads}") String root) {
        this.rootDir = Paths.get(root).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile saveInternDocument(Long internId, DocumentType type, MultipartFile file) {
        try {
            Files.createDirectories(rootDir);

            String original = StringUtils
                    .cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0)
                ext = original.substring(dot);

            String filename = type.name().toLowerCase() + "-" + UUID.randomUUID() + ext;
            Path dir = rootDir.resolve("interns").resolve(String.valueOf(internId));
            Files.createDirectories(dir);

            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // Lưu path tương đối
            String fileUrl = "interns/" + internId + "/" + filename;
            long size = Files.size(target);

            return new StoredFile(fileUrl, filename, size);
        } catch (Exception e) {
            throw new FileStorageException("Không thể lưu file", e);
        }
    }

    @Override
    public StoredFile loadAsResource(String fileUrl) {
        try {
            Path path = rootDir.resolve(fileUrl).normalize();
            if (!Files.exists(path))
                throw new NotFoundException("Không tìm thấy file: " + fileUrl);
            long size = Files.size(path);
            return new StoredFile(fileUrl, path.getFileName().toString(), size);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Không thể tải file", e);
        }
    }

    @Override
    public org.springframework.core.io.Resource loadFileAsResource(String fileUrl) {
        try {
            Path path = rootDir.resolve(fileUrl).normalize();
            if (!Files.exists(path))
                throw new NotFoundException("Không tìm thấy file: " + fileUrl);
            org.springframework.core.io.UrlResource resource = new org.springframework.core.io.UrlResource(
                    path.toUri());
            if (!resource.exists() || !resource.isReadable())
                throw new FileStorageException("Không thể đọc file: " + fileUrl);
            return resource;
        } catch (NotFoundException | FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("Không thể tải file dưới dạng resource", e);
        }
    }
}
