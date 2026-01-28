package se.fastighet.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCommentRequest {

    @NotBlank(message = "Kommentar krävs")
    @Size(min = 1, max = 2000, message = "Kommentaren måste vara mellan 1 och 2000 tecken")
    private String content;

    @Builder.Default
    private boolean internal = false; // Intern kommentar (syns bara för styrelse/tekniker)
}
