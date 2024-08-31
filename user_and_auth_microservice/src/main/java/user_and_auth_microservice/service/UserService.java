package user_and_auth_microservice.service;

import lombok.RequiredArgsConstructor;
import user_and_auth_microservice.dtos.*;
import user_and_auth_microservice.exception.AppException;
import user_and_auth_microservice.mapper.UserMapper;
import user_and_auth_microservice.model.Role;
import user_and_auth_microservice.model.User;
import user_and_auth_microservice.repository.UserRepository;
import user_and_auth_microservice.utils.PasswordUtil;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BlacklistService blacklistService;

    public UserDTO login(LoginDTO loginDTO) {
        User user = userRepository.findByEmailOrUsername(loginDTO.login())
                .orElseThrow(() -> new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        if (PasswordUtil.verifyPassword(loginDTO.password(), user.getPassword())) {
            return userMapper.toUser(user);
        }

        throw new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED);
    }

    public RegisterResponseDTO register(RegisterDTO registerDTO) {
        userRepository.findByEmailOrUsernameForRegistration(registerDTO.email(), registerDTO.username())
                .ifPresent(user -> {
                    if (user.getEmail().equals(registerDTO.email())) {
                        throw new AppException("Email already in use", HttpStatus.BAD_REQUEST);
                    } else if (user.getUsername().equals(registerDTO.username())) {
                        throw new AppException("Username already in use", HttpStatus.BAD_REQUEST);
                    }
                });

        User user = userMapper.registerToUser(registerDTO);
        user.setPassword(PasswordUtil.hashPassword(registerDTO.password()));

        if (registerDTO.role() == null || registerDTO.role().isEmpty()) {
            user.setRole(Role.USER);
        } else {
            try {
                user.setRole(Role.valueOf(registerDTO.role().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new AppException("Invalid role provided", HttpStatus.BAD_REQUEST);
            }
        }

        userRepository.save(user);

        RegisterResponseDTO responseDTO = new RegisterResponseDTO();
        responseDTO.setMessage("Welcome");

        return responseDTO;
    }

    
    public UserDTO getUser(String accountId) {
        User user = userRepository.findById(accountId).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        return userMapper.toUser(user);

    }
    
    public void deleteUser(String accountId) {
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.BAD_REQUEST));

        try {
            userRepository.delete(user);
            blacklistLogin(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            throw new AppException("Error deleting user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public UserDTO updateEmail(String accountId, String newEmail) {
        try {
            User user = userRepository.findById(accountId)
                    .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
            blacklistService.blacklistByLogin(user.getEmail());
            user.setEmail(newEmail);
            User updatedUser = userRepository.save(user);
            return userMapper.toUser(updatedUser);
        } catch (Exception e) {
            throw new AppException("Error" + e.getMessage(), HttpStatus.valueOf(422));
        }
    }

    public UserDTO updateUsername(String accountId, String newUsername) {
        try {
            User user = userRepository.findById(accountId)
                    .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
            blacklistService.blacklistByLogin(user.getUsername());
            user.setUsername(newUsername);
            User updatedUser = userRepository.save(user);
            return userMapper.toUser(updatedUser);
        } catch (Exception e) {
            throw new AppException("Error" + e.getMessage(), HttpStatus.valueOf(422));
        }
    }

    public String updateDisplayName(String accountId, String newDisplayname) {
        try {
            User user = userRepository.findById(accountId)
                    .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
            user.setDisplayName(newDisplayname);
            userRepository.save(user);
            return newDisplayname;
        } catch (Exception e) {
            throw new AppException("Error" + e.getMessage(), HttpStatus.valueOf(422));
        }
    }

    public String updatePassword(String accountId, String newPassword) {
        try {
            User user = userRepository.findById(accountId)
                    .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
            user.setPassword(PasswordUtil.hashPassword(newPassword));
            userRepository.save(user);
            return "Password was updated";
        } catch (Exception e) {
            throw new AppException("Error" + e.getMessage(), HttpStatus.valueOf(422));
        }
    }

    public String updateRole(String senderId, UpdateRoleDTO updateRoleDTO) {
        User admin = userRepository.findById(senderId).orElseThrow(() -> new AppException("Unauthorized", HttpStatus.UNAUTHORIZED));
        if(!admin.getRole().toString().equals("ADMIN")) {
            throw new AppException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(updateRoleDTO.accountId()).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        
        try {
            user.setRole(Role.valueOf(updateRoleDTO.newRole().toUpperCase()));
            userRepository.save(user);
            return "Done";
        } catch (Exception e) {
            throw new AppException("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

    }

    public PaginatedUserSearchResponseDTO searchUsers(UserSearchRequestDTO request) {
        // Perform the search operation based on the search term and pagination parameters
        Page<User> usersPage = userRepository.findBySearchTerm(
            request.searchTerm(), 
            PageRequest.of(request.page(), request.size())
        );
    
        // Map the result to a list of UserSearchResponseDTO
        List<UserSearchResponseDTO> userDTOs = usersPage.getContent().stream()
            .map(user -> UserSearchResponseDTO.builder()
                .id(user.getId())  // Assuming id is a String in User, no need for .toString()
                .displayName(user.getDisplayName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())  // Convert Role enum to String using .name()
                .build())
            .collect(Collectors.toList());
    
        // Return the paginated response DTO
        return PaginatedUserSearchResponseDTO.builder()
            .content(userDTOs)
            .page(usersPage.getNumber())
            .size(usersPage.getSize())
            .totalElements(usersPage.getTotalElements())
            .totalPages(usersPage.getTotalPages())
            .last(usersPage.isLast())
            .build();
    }    

    private void blacklistLogin(String email, String username) {
        blacklistService.blacklistByLogin(email);
        blacklistService.blacklistByLogin(username);
    }

    public void ValidateUser(String senderId, String accountId) {
        User user = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException("Error fetching the sender", HttpStatus.BAD_REQUEST));

                if (!user.getRole().toString().equals("ADMIN") && !user.getId().equals(accountId)) {
                    throw new AppException("Unauthorized", HttpStatus.UNAUTHORIZED);
                }
    }
}


