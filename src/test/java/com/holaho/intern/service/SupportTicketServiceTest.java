package com.holaho.intern.service;

import com.holaho.intern.entity.SupportTicket;
import com.holaho.intern.entity.TicketComment;
import com.holaho.intern.repository.SupportTicketRepository;
import com.holaho.intern.repository.TicketAttachmentRepository;
import com.holaho.intern.repository.TicketCommentRepository;
import com.holaho.intern.repository.TicketStatusHistoryRepository;
import com.holaho.intern.shared.dto.request.SupportTicketCreateRequest;
import com.holaho.intern.shared.enums.TicketCategory;
import com.holaho.intern.shared.enums.TicketPriority;
import com.holaho.intern.shared.enums.TicketStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ForbiddenException;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

    @Mock
    private SupportTicketRepository ticketRepository;
    @Mock
    private TicketCommentRepository commentRepository;
    @Mock
    private TicketAttachmentRepository attachmentRepository;
    @Mock
    private TicketStatusHistoryRepository statusHistoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SupportTicketService ticketService;

    private User internUser;
    private User hrUser;

    @BeforeEach
    void setUp() {
        internUser = new User();
        internUser.setId(10L);
        internUser.setFullName("Thuc Tap Sinh A");

        hrUser = new User();
        hrUser.setId(20L);
        hrUser.setFullName("Nhan Su HR B");
    }

    @Test
    @DisplayName("Create ticket: Should create ticket with OPEN status and 3-day SLA due date")
    void testCreateTicketSuccess() {
        SupportTicketCreateRequest request = new SupportTicketCreateRequest();
        request.setCategory(TicketCategory.CERTIFICATE);
        request.setPriority(TicketPriority.HIGH);
        request.setTitle("Xin cap gia xac nhan thuc tap");
        request.setContent("Noi dung chi tiet yeu cau ho tro");

        when(userRepository.findById(10L)).thenReturn(Optional.of(internUser));
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(100L);
            return t;
        });

        SupportTicket created = ticketService.createTicket(request, 10L);

        assertNotNull(created);
        assertEquals(TicketStatus.OPEN, created.getStatus());
        assertEquals(TicketPriority.HIGH, created.getPriority());
        assertNotNull(created.getFirstResponseDueAt());
        verify(statusHistoryRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Assign ticket: Should assign HR user and change OPEN status to IN_PROGRESS")
    void testAssignTicketSuccess() {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(100L);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(internUser);

        when(ticketRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(20L)).thenReturn(Optional.of(hrUser));
        when(userRepository.findById(20L)).thenReturn(Optional.of(hrUser));
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupportTicket assigned = ticketService.assignTicket(100L, 20L, 20L);

        assertEquals(hrUser, assigned.getAssignedTo());
        assertEquals(TicketStatus.IN_PROGRESS, assigned.getStatus());
        verify(statusHistoryRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Resolve ticket: Should update status to RESOLVED and set resolution text")
    void testResolveTicketSuccess() {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(100L);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setCreatedBy(internUser);

        when(ticketRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(20L)).thenReturn(Optional.of(hrUser));
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupportTicket resolved = ticketService.resolveTicket(100L, "Da cap giay xac nhan", 20L);

        assertEquals(TicketStatus.RESOLVED, resolved.getStatus());
        assertEquals("Da cap giay xac nhan", resolved.getResolution());
        assertNotNull(resolved.getResolvedAt());
    }

    @Test
    @DisplayName("Resolve closed ticket: Should throw BadRequestException")
    void testResolveClosedTicketThrowsException() {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(100L);
        ticket.setStatus(TicketStatus.CLOSED);

        when(ticketRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(20L)).thenReturn(Optional.of(hrUser));

        assertThrows(BadRequestException.class, () -> ticketService.resolveTicket(100L, "Resolution", 20L));
    }

    @Test
    @DisplayName("Add internal comment: Intern posting internal note should throw ForbiddenException")
    void testInternPostingInternalCommentThrowsForbidden() {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(100L);
        ticket.setCreatedBy(internUser);

        when(ticketRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(10L)).thenReturn(Optional.of(internUser));

        assertThrows(ForbiddenException.class, () -> 
            ticketService.addComment(100L, 10L, "Ghi chu noi bo", true, false)
        );
    }
}
