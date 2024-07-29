package user_and_auth_microservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import user_and_auth_microservice.dtos.LoginDTO;
import user_and_auth_microservice.dtos.RegisterDTO;
import user_and_auth_microservice.dtos.RegisterResponseDTO;
import user_and_auth_microservice.dtos.UpdateRoleDTO;
import user_and_auth_microservice.dtos.UpdateUserDTO;
import user_and_auth_microservice.dtos.UserDTO;
import user_and_auth_microservice.service.UserAuthProviderService;
import user_and_auth_microservice.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final UserAuthProviderService userAuthProvider;

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        UserDTO responseDTO = userService.login(loginDTO);
        String token = userAuthProvider.createToken(responseDTO);
        responseDTO.setToken(token);
        response.addCookie(userAuthProvider.createJwtCookie(token, 10803));

        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        RegisterResponseDTO responseDTO = userService.register(registerDTO);
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/delete/{accountid}")
    public ResponseEntity<String> deleteAccount(@Valid @PathVariable String accountid,
            HttpServletResponse response,
            HttpServletRequest request) {

        String senderId = request.getHeader("UserId");

        userService.ValidateUser(senderId, accountid);
        userService.deleteUser(accountid);

        if (senderId.equals(accountid)) {
            response.addCookie(userAuthProvider.createJwtCookie(null, 0));
        }

        return ResponseEntity.ok().body("Done");
    }

    @PutMapping("/update")
    public ResponseEntity<String> updateUserDetails(@Valid @RequestBody UpdateUserDTO updateUserDTO,
            HttpServletResponse response,
            HttpServletRequest request) {
        
        String senderId = request.getHeader("UserId");
        
        userService.ValidateUser(senderId, updateUserDTO.accountId());

        String fieldToUpdate = updateUserDTO.informationToUpdate();
        String newInformation = updateUserDTO.newInformation();
        String responseInfo = null;

        switch (fieldToUpdate) {
            case "email":
                if (!newInformation.matches(".+@.+\\..+")) {
                    return ResponseEntity.badRequest().body("Invalid email format");
                }
                // Call service to update email
                UserDTO newEmail = userService.updateEmail(updateUserDTO.accountId(), newInformation);
                String newEmailToken = userAuthProvider.createToken(newEmail);
                response.addCookie(userAuthProvider.createJwtCookie(newEmailToken, 10803));
                response.addHeader("newJWT", newEmailToken);
                responseInfo = newInformation;
                break;

            case "username":
                if (newInformation.length() < 4 || newInformation.length() > 20) {
                    return ResponseEntity.badRequest().body("Username must be between 4 and 20 characters");
                }
                // Call service to update username
                UserDTO newUsername = userService.updateUsername(updateUserDTO.accountId(), newInformation);
                String newUsernameToken = userAuthProvider.createToken(newUsername);
                response.addCookie(userAuthProvider.createJwtCookie(newUsernameToken, 10803));
                response.addHeader("newJWT", newUsernameToken);
                responseInfo = newInformation;
                break;

            case "displayName":
                if (newInformation.length() < 4 || newInformation.length() > 50) {
                    return ResponseEntity.badRequest().body("Display name must be between 4 and 50 characters");
                }
                // Call service to update display name
                userService.updateDisplayName(updateUserDTO.accountId(), newInformation);
                responseInfo = newInformation;
                break;

            case "password":
                if (!newInformation.matches("^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$")) {
                    return ResponseEntity.badRequest().body(
                            "Password must be between 8 and 16 characters, contain at least one uppercase letter, one number, and one special character");
                }
                userService.updatePassword(updateUserDTO.accountId(), newInformation);
                responseInfo = newInformation;
                break;

            default:
                return ResponseEntity.badRequest().body("Invalid field to update");
        }

        return ResponseEntity.ok(newInformation);
    }

    @GetMapping("/profile/{accountid}")
    public ResponseEntity<UserDTO> getProfile(@PathVariable String accountid, HttpServletRequest request) {
        String senderId = request.getHeader("UserId");
        
        userService.ValidateUser(senderId, accountid);

        return ResponseEntity.ok().body( userService.getUser(accountid));
    }

    @PutMapping("/updaterole")
    public ResponseEntity<String> updateRole(@Valid @RequestBody UpdateRoleDTO updateRoleDTO, HttpServletRequest request) {
        String senderId = request.getHeader("UserId");
        userService.updateRole(senderId, updateRoleDTO);

        return ResponseEntity.ok().body("User was updated");
    }
}
