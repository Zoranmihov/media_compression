package user_and_auth_microservice.dtos;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDTO(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 4, max = 20) String username,
        @NotBlank @Size(min = 4, max = 50) String displayName,
        @Nullable String role,
        @NotNull 
        @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters long")
        @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$",
            message = "Password must contain at least one uppercase letter, one number, and one special character"
        ) String password) {
}
