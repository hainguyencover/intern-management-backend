package com.holaho.intern.repository;

import com.holaho.intern.entity.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    List<TicketAttachment> findByTicketIdAndDeletedAtIsNull(Long ticketId);
    List<TicketAttachment> findByCommentIdAndDeletedAtIsNull(Long commentId);
}
