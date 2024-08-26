package user_and_auth_microservice.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import user_and_auth_microservice.dtos.UserDTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;

@RequiredArgsConstructor
@Component
public class UserAuthProviderService {

    @Value("${jwt_secret:default-secret-key}")
    private String secretKey;

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createToken(UserDTO userDTO) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + 10801000);

        return JWT.create()
                .withIssuer(userDTO.getUsername())
                .withIssuedAt(now)
                .withExpiresAt(validity)
                .withClaim("id", userDTO.getId())
                .withClaim("email", userDTO.getEmail())
                .withClaim("role", userDTO.getRole())
                .sign(Algorithm.HMAC256(secretKey));
    }

    public Cookie createJwtCookie(String token, int maxAge) {
        Cookie jwtCookie = new Cookie("JWT", token);
        jwtCookie.setHttpOnly(true);
        //TODO: Set cookie to true for production
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(maxAge);
        return jwtCookie;
    }

}
