package se.fastighet.core.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.fastighet.core.entity.Ticket;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketRequest {

    @Size(min = 5, max = 255, message = "Titeln måste vara mellan 5 och 255 tecken")
    private String title;

    @Size(min = 10, max = 5000, message = "Beskrivningen måste vara mellan 10 och 5000 tecken")
    private String description;

    private Long categoryId;

    private Ticket.Priority priority;

    private Ticket.Status status;
}
