package user_and_auth_microservice.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;

public record UserSearchRequestDTO(
    @NotEmpty(message = "Search term must not be empty") String searchTerm,
    @Min(value = 0, message = "Page number must be 0 or greater") int page,
    @Min(value = 1, message = "Size must be 1 or greater") int size
) {
}
