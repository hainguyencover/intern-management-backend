package com.holaho.intern.service;

import com.holaho.intern.shared.dto.StoredFile;


import com.holaho.intern.shared.dto.response.InternDocumentResponse;
import com.holaho.intern.shared.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InternDocumentService {
    InternDocumentResponse uploadForIntern(Long internId, DocumentType type, MultipartFile file, Long uploaderId);

    List<InternDocumentResponse> getMyDocuments(Long internId);

    List<InternDocumentResponse> getDocumentsOfIntern(Long internId);

    InternDocumentResponse approve(Long documentId, Long hrUserId);

    InternDocumentResponse reject(Long documentId, Long hrUserId, String note);

    InternDocumentResponse confirmContract(Long internId, Long documentId);

    com.holaho.intern.shared.dto.StoredFile download(Long documentId, Long requesterUserId, boolean isHr);

    // Optionally: download or other admin actions
}

