package com.holaho.intern.intern.service;

import com.holaho.intern.shared.dto.StoredFile;
import com.holaho.intern.shared.dto.response.InternDocumentResponse;
import com.holaho.intern.shared.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InternDocumentService {
    InternDocumentResponse uploadForIntern(Long internId, DocumentType type, MultipartFile file, Long uploaderId);

    List<InternDocumentResponse> getMyDocuments(Long internId);

    List<InternDocumentResponse> getDocumentsOfIntern(Long internId);

    Page<InternDocumentResponse> getDocumentsForHr(String status, String documentType, Long tenantId, Pageable pageable);

    InternDocumentResponse approve(Long documentId, Long hrUserId);

    InternDocumentResponse reject(Long documentId, Long hrUserId, String reason);

    InternDocumentResponse confirmContract(Long internId, Long documentId);

    StoredFile download(Long documentId, Long requesterUserId, boolean isHr);

    boolean hasAllRequiredDocuments(Long internId);
}
