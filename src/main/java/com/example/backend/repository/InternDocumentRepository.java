package com.example.backend.repository;

import com.example.backend.entity.InternDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InternDocumentRepository extends JpaRepository<InternDocument, Long> {
    List<InternDocument> findByIntern_Id(Long internId);

    List<InternDocument> findByIntern_IdOrderByUploadedAtDesc(Long internId);

    Optional<InternDocument> findTopByIntern_IdAndTypeOrderByUploadedAtDesc(Long internId, String type);

    List<InternDocument> findByStatusOrderByUploadedAtDesc(String status);

    boolean existsByIntern_IdAndType(Long internId, String type);

    boolean existsByIdAndIntern_Id(Long id, Long internId);
}
