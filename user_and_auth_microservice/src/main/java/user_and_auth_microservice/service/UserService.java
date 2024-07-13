package user_and_auth_microservice.service;

import lombok.RequiredArgsConstructor;
import user_and_auth_microservice.dtos.LoginDTO;
import user_and_auth_microservice.dtos.RegisterDTO;
import user_and_auth_microservice.dtos.RegisterResponseDTO;
import user_and_auth_microservice.dtos.UserDTO;
import user_and_auth_microservice.exception.AppException;
import user_and_auth_microservice.mapper.UserMapper;
import user_and_auth_microservice.model.User;
import user_and_auth_microservice.repository.UserRepository;
import user_and_auth_microservice.utils.PasswordUtil;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserDTO login(LoginDTO loginDTO) {
        User user = userRepository.findByEmailOrUsername(loginDTO.login())
                .orElseThrow(() -> new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        if (PasswordUtil.verifyPassword(loginDTO.password(), user.getPassword())) {
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
        user.setPassword(PasswordUtil.hashPassword(registerDTO.password()));
        userRepository.save(user);
        RegisterResponseDTO responseDTO = new RegisterResponseDTO();
        responseDTO.setMessage("Welcome");
        return responseDTO;
    }
}
