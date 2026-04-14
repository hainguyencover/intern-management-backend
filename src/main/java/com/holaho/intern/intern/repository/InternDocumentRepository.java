package com.holaho.intern.intern.repository;

import com.holaho.intern.intern.entity.InternDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternDocumentRepository extends JpaRepository<InternDocument, Long> {
    List<InternDocument> findByInternId(Long internId);

    @Query(value = "SELECT d FROM InternDocument d JOIN FETCH d.intern i JOIN FETCH i.user WHERE d.status = 'PENDING' ORDER BY d.uploadedAt DESC", countQuery = "SELECT count(d) FROM InternDocument d WHERE d.status = 'PENDING'")
    Page<InternDocument> findPendingDocuments(Pageable pageable);

    @Query("SELECT d FROM InternDocument d JOIN FETCH d.intern i JOIN FETCH i.user WHERE d.id = :id")
    Optional<InternDocument> findByIdWithIntern(@Param("id") Long id);

    List<InternDocument> findByInternIdOrderByUploadedAtDesc(Long internId);

    List<InternDocument> findByIntern_IdOrderByUploadedAtDesc(Long internId);

    Optional<InternDocument> findTopByIntern_IdAndTypeOrderByUploadedAtDesc(Long internId, String type);

    List<InternDocument> findByStatusOrderByUploadedAtDesc(String status);

    boolean existsByIntern_IdAndType(Long internId, String type);

    boolean existsByIdAndIntern_Id(Long id, Long internId);

    List<InternDocument> findByStatus(String status);

    Page<InternDocument> findByStatus(String status, Pageable pageable);

    List<InternDocument> findByInternIdAndStatus(Long internId, String status);

    List<InternDocument> findByType(String type);

    List<InternDocument> findByInternIdAndType(Long internId, String type);

    @Query("SELECT d FROM InternDocument d WHERE d.reviewedBy.id = :reviewerId")
    List<InternDocument> findByReviewedById(@Param("reviewerId") Long reviewerId);

    long countByStatus(String status);

    long countByInternIdAndStatus(Long internId, String status);

    boolean existsByInternIdAndType(Long internId, String type);
}

