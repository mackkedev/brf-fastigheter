package se.fastighet.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private EventType eventType;
    private Long ticketId;
    private String ticketTitle;
    private UUID propertyId;
    private String propertyName;
    private UUID reporterId;
    private String reporterName;
    private String reporterEmail;
    private UUID assigneeId;
    private String assigneeName;
    private String assigneeEmail;
    private String oldStatus;
    private String newStatus;
    private String comment;
    private UUID changedById;
    private String changedByName;
    private LocalDateTime timestamp;

    public enum EventType {
        TICKET_CREATED,
        TICKET_STATUS_CHANGED,
        TICKET_ASSIGNED,
        TICKET_COMMENT_ADDED,
        TICKET_ESCALATED
    }
}
