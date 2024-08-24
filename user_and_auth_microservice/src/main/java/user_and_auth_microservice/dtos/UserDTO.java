package user_and_auth_microservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {
    
    private String id;
    private String displayName;
    private String username;
    private String email;
    private String role;
    private String token;
    private long tokenExpirationTime;
}
