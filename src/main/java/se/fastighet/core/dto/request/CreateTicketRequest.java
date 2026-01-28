package se.fastighet.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.fastighet.core.entity.Ticket;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    @NotBlank(message = "Titel krävs")
    @Size(min = 5, max = 255, message = "Titeln måste vara mellan 5 och 255 tecken")
    private String title;

    @NotBlank(message = "Beskrivning krävs")
    @Size(min = 10, max = 5000, message = "Beskrivningen måste vara mellan 10 och 5000 tecken")
    private String description;

    private Long categoryId;

    @Builder.Default
    private Ticket.Priority priority = Ticket.Priority.MEDIUM;

    @NotNull(message = "Fastighets-ID krävs")
    private UUID propertyId;

    private UUID unitId; // Valfritt - om ärendet gäller en specifik lägenhet
}
