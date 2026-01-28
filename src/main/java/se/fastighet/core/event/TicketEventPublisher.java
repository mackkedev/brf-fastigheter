package se.fastighet.core.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import se.fastighet.core.entity.Ticket;
import se.fastighet.core.entity.TicketComment;
import se.fastighet.core.entity.User;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketEventPublisher {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${jms.queue.ticket-events}")
    private String ticketEventsQueue;

    public void publishTicketCreated(Ticket ticket) {
        TicketEvent event = TicketEvent.builder()
                .eventType(TicketEvent.EventType.TICKET_CREATED)
                .ticketId(ticket.getId())
                .ticketTitle(ticket.getTitle())
                .propertyId(ticket.getProperty().getId())
                .propertyName(ticket.getProperty().getName())
                .reporterId(ticket.getReporter().getId())
                .reporterName(ticket.getReporter().getName())
                .reporterEmail(ticket.getReporter().getEmail())
                .timestamp(LocalDateTime.now())
                .build();

        sendEvent(event);
        log.info("Published TICKET_CREATED event for ticket {}", ticket.getId());
    }

    public void publishStatusChanged(Ticket ticket, String oldStatus, User changedBy) {
        TicketEvent event = TicketEvent.builder()
                .eventType(TicketEvent.EventType.TICKET_STATUS_CHANGED)
                .ticketId(ticket.getId())
                .ticketTitle(ticket.getTitle())
                .propertyId(ticket.getProperty().getId())
                .propertyName(ticket.getProperty().getName())
                .reporterId(ticket.getReporter().getId())
                .reporterName(ticket.getReporter().getName())
                .reporterEmail(ticket.getReporter().getEmail())
                .oldStatus(oldStatus)
                .newStatus(ticket.getStatus().name())
                .changedById(changedBy.getId())
                .changedByName(changedBy.getName())
                .timestamp(LocalDateTime.now())
                .build();

        if (ticket.getAssignee() != null) {
            event.setAssigneeId(ticket.getAssignee().getId());
            event.setAssigneeName(ticket.getAssignee().getName());
            event.setAssigneeEmail(ticket.getAssignee().getEmail());
        }

        sendEvent(event);
        log.info("Published TICKET_STATUS_CHANGED event for ticket {} ({} -> {})",
                ticket.getId(), oldStatus, ticket.getStatus());
    }

    public void publishTicketAssigned(Ticket ticket, User assignedBy) {
        TicketEvent event = TicketEvent.builder()
                .eventType(TicketEvent.EventType.TICKET_ASSIGNED)
                .ticketId(ticket.getId())
                .ticketTitle(ticket.getTitle())
                .propertyId(ticket.getProperty().getId())
                .propertyName(ticket.getProperty().getName())
                .reporterId(ticket.getReporter().getId())
                .reporterName(ticket.getReporter().getName())
                .reporterEmail(ticket.getReporter().getEmail())
                .assigneeId(ticket.getAssignee().getId())
                .assigneeName(ticket.getAssignee().getName())
                .assigneeEmail(ticket.getAssignee().getEmail())
                .changedById(assignedBy.getId())
                .changedByName(assignedBy.getName())
                .timestamp(LocalDateTime.now())
                .build();

        sendEvent(event);
        log.info("Published TICKET_ASSIGNED event for ticket {} to {}",
                ticket.getId(), ticket.getAssignee().getName());
    }

    public void publishCommentAdded(Ticket ticket, TicketComment comment) {
        if (comment.isInternal()) {
            log.debug("Skipping event for internal comment on ticket {}", ticket.getId());
            return;
        }

        TicketEvent event = TicketEvent.builder()
                .eventType(TicketEvent.EventType.TICKET_COMMENT_ADDED)
                .ticketId(ticket.getId())
                .ticketTitle(ticket.getTitle())
                .propertyId(ticket.getProperty().getId())
                .propertyName(ticket.getProperty().getName())
                .reporterId(ticket.getReporter().getId())
                .reporterName(ticket.getReporter().getName())
                .reporterEmail(ticket.getReporter().getEmail())
                .comment(comment.getContent())
                .changedById(comment.getAuthor().getId())
                .changedByName(comment.getAuthor().getName())
                .timestamp(LocalDateTime.now())
                .build();

        if (ticket.getAssignee() != null) {
            event.setAssigneeId(ticket.getAssignee().getId());
            event.setAssigneeName(ticket.getAssignee().getName());
            event.setAssigneeEmail(ticket.getAssignee().getEmail());
        }

        sendEvent(event);
        log.info("Published TICKET_COMMENT_ADDED event for ticket {}", ticket.getId());
    }

    public void publishTicketEscalated(Ticket ticket) {
        TicketEvent event = TicketEvent.builder()
                .eventType(TicketEvent.EventType.TICKET_ESCALATED)
                .ticketId(ticket.getId())
                .ticketTitle(ticket.getTitle())
                .propertyId(ticket.getProperty().getId())
                .propertyName(ticket.getProperty().getName())
                .reporterId(ticket.getReporter().getId())
                .reporterName(ticket.getReporter().getName())
                .reporterEmail(ticket.getReporter().getEmail())
                .timestamp(LocalDateTime.now())
                .build();

        sendEvent(event);
        log.info("Published TICKET_ESCALATED event for ticket {}", ticket.getId());
    }

    private void sendEvent(TicketEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            jmsTemplate.convertAndSend(ticketEventsQueue, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ticket event", e);
            throw new RuntimeException("Failed to publish ticket event", e);
        }
    }
}
