package user_and_auth_microservice.dtos;

import jakarta.validation.constraints.NotEmpty;

public record UpdateUserDTO(
    @NotEmpty(message = "Field was empty") String informationToUpdate,
    @NotEmpty(message = "Field was empty") String newInformation,
    @NotEmpty(message = "Field was empty") String accountId
) {
    
}
