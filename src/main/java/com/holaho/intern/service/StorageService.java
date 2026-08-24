package com.holaho.intern.service;

import com.holaho.intern.shared.dto.StoredFile;
import com.holaho.intern.shared.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    StoredFile saveInternDocument(Long internId, DocumentType type, MultipartFile file);

    StoredFile loadAsResource(String fileUrl);

    org.springframework.core.io.Resource loadFileAsResource(String fileUrl);

    void deleteFile(String fileUrl);
}
