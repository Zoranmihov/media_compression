package user_and_auth_microservice.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import user_and_auth_microservice.config.UserAuthProvider;
import user_and_auth_microservice.dtos.LoginDTO;
import user_and_auth_microservice.dtos.RegisterDTO;
import user_and_auth_microservice.dtos.RegisterResponseDTO;
import user_and_auth_microservice.dtos.UserDTO;
import user_and_auth_microservice.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserAuthProvider userAuthProvider;

    @PostMapping("/login")
    public ResponseEntity<UserDTO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response) {
        UserDTO responseDTO = userService.login(loginDTO);
        String token = userAuthProvider.createToken(responseDTO);
        responseDTO.setToken(token);
        Cookie jwtCookie = new Cookie("JWT", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(10803);

        response.addCookie(jwtCookie);

        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        RegisterResponseDTO responseDTO = userService.register(registerDTO);
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/profile")
    public ResponseEntity<String> getProfile() {
        return ResponseEntity.ok("Works");
    }

    @GetMapping("/admin")
    public ResponseEntity<String> getProfileAdmin() {
        return ResponseEntity.ok("Works");
    }
}
