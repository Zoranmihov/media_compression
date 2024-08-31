package user_and_auth_microservice.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaginatedUserSearchResponseDTO {
    private List<UserSearchResponseDTO> content;  // List of users for the current page
    private int page;                             // Current page number
    private int size;                             // Size of each page (e.g., 20 users)
    private long totalElements;                   // Total number of users found
    private int totalPages;                       
    private boolean last;                         
}
