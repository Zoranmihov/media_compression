package spring_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import spring_backend.dtos.LoginDTO;
import spring_backend.dtos.RegisterDTO;
import spring_backend.dtos.RegisterResponseDTO;
import spring_backend.dtos.UserDTO;
import spring_backend.exception.AppException;
import spring_backend.mapper.UserMapper;
import spring_backend.model.User;
import spring_backend.repository.UserRepository;

import java.nio.CharBuffer;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserDTO login(LoginDTO loginDTO) {
        User user = userRepository.findByEmailOrUsername(loginDTO.login())
                .orElseThrow(() -> new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        if (passwordEncoder.matches(CharBuffer.wrap(loginDTO.password()), user.getPassword())) {
            return userMapper.toUser(user);
        }

        throw new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED);
    }

    public RegisterResponseDTO register(RegisterDTO registerDTO){

        userRepository.findByEmailOrUsernameForRegistration(registerDTO.email(), registerDTO.username()).ifPresent(user -> {
            if (user.getEmail().equals(registerDTO.email())) {
                throw new AppException("Email already in use", HttpStatus.BAD_REQUEST);
            } else if (user.getUsername().equals(registerDTO.username())) {
                throw new AppException("Username already in use", HttpStatus.BAD_REQUEST);
            }
        });

        User user = userMapper.registerToUser(registerDTO);
        user.setPassword(passwordEncoder.encode(CharBuffer.wrap(registerDTO.password())));
        userRepository.save(user);
        RegisterResponseDTO responseDTO = new RegisterResponseDTO();
        responseDTO.setMessage("Welcome");
        return responseDTO;
    }
}
