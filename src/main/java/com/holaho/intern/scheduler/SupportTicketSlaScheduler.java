package com.holaho.intern.scheduler;

import com.holaho.intern.entity.SupportTicket;
import com.holaho.intern.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupportTicketSlaScheduler {

    private final SupportTicketRepository ticketRepository;

    /**
     * Checks for support tickets that have passed their firstResponseDueAt SLA limit without response.
     * Runs every 15 minutes.
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void monitorOverdueTickets() {
        LocalDateTime now = LocalDateTime.now();
        List<SupportTicket> overdueTickets = ticketRepository.findOverdueTickets(now);
        if (!overdueTickets.isEmpty()) {
            log.warn("SLA Alert: Found {} support tickets breaching SLA 3-working-day response limit!", overdueTickets.size());
            for (SupportTicket ticket : overdueTickets) {
                log.warn("Ticket [Code: {}, ID: {}] created at {} is OVERDUE for first response (Due: {})",
                        ticket.getTicketCode(), ticket.getId(), ticket.getCreatedAt(), ticket.getFirstResponseDueAt());
            }
        }
    }
}
