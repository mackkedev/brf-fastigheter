package se.fastighet.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.fastighet.core.entity.Ticket;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketListResponse {

    private Long id;
    private String title;
    private String categoryName;
    private String categoryIcon;
    private Ticket.Status status;
    private Ticket.Priority priority;
    private String reporterName;
    private String assigneeName;
    private String propertyName;
    private String unitNumber;
    private int commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
