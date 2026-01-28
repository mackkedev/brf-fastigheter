package se.fastighet.core.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "E-post är obligatorisk")
    @Email(message = "Ogiltig e-postadress")
    private String email;

    @NotBlank(message = "Lösenord är obligatoriskt")
    private String password;
}
