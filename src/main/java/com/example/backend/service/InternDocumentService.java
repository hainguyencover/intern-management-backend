package com.example.backend.service;

import com.example.backend.dto.response.InternDocumentResponse;
import com.example.backend.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InternDocumentService {
    InternDocumentResponse uploadForIntern(Long internId, DocumentType type, MultipartFile file, Long uploaderId);

    List<InternDocumentResponse> getMyDocuments(Long internId);

    List<InternDocumentResponse> getDocumentsOfIntern(Long internId);

    InternDocumentResponse approve(Long documentId, Long hrUserId);

    InternDocumentResponse reject(Long documentId, Long hrUserId, String note);

    InternDocumentResponse confirmContract(Long internId, Long documentId);

    com.example.backend.dto.StoredFile download(Long documentId, Long requesterUserId, boolean isHr);

    // Optionally: download or other admin actions
}
