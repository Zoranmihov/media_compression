package user_and_auth_microservice.dtos;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleDTO(
    @NotBlank(message = "Field is missing") String accountId,
    @NotBlank(message = "Field is missing") String newRole
) {
} 