package spring_backend.dtos;

import jakarta.validation.constraints.Size;

public record LoginDTO(
        @Size(min = 4, max = 30) String login,
        @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters long") String password) {
}
