package com.example.backend.service;

import com.example.backend.dto.StoredFile;
import com.example.backend.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    StoredFile saveInternDocument(Long internId, DocumentType type, MultipartFile file);

    StoredFile loadAsResource(String fileUrl);

    // Return a Spring Resource for streaming file contents
    org.springframework.core.io.Resource loadFileAsResource(String fileUrl);
}
