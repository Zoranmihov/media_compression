package gateway_microservice.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import gateway_microservice.dtos.UserDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@Component
public class UserAuthProviderService {

    @Autowired
    private BlacklistService blacklistService;

    @Value("${jwt_secret:default-secret-key}")
    private String secretKey;

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public Mono<Authentication> validateTokenAsync(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secretKey)).build();
            DecodedJWT decodedJWT = verifier.verify(token);

            UserDTO user = UserDTO.builder()
                .username(decodedJWT.getIssuer())
                .email(decodedJWT.getClaim("email").asString())
                .role(decodedJWT.getClaim("role").asString())
                .build();

            // Check if email or username is blacklisted
            return Mono.zip(
                    blacklistService.isBlacklisted(user.getEmail()),
                    blacklistService.isBlacklisted(user.getUsername())
            ).flatMap(tuple -> {
                Boolean isEmailBlacklisted = tuple.getT1();
                Boolean isUsernameBlacklisted = tuple.getT2();

                if (isEmailBlacklisted || isUsernameBlacklisted) {
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is blacklisted"));
                }
                
                return Mono.just((Authentication) new UsernamePasswordAuthenticationToken(user, token, Collections.singletonList(() -> "ROLE_" + user.getRole())));
            }).switchIfEmpty(Mono.just((Authentication) new UsernamePasswordAuthenticationToken(user, token, Collections.singletonList(() -> "ROLE_" + user.getRole()))));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public String refreshToken(String oldToken) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secretKey)).build();
        DecodedJWT decodedJWT = verifier.verify(oldToken);

        Date now = new Date();
        Date expiresAt = decodedJWT.getExpiresAt();

        long timeToExpiry = expiresAt.getTime() - now.getTime();
        if (timeToExpiry > 20 * 60 * 1000) { // Refresh if less than 20 minutes left
            return oldToken;
        }

        Date validity = new Date(now.getTime() + 10800000); // 3 hours

        return JWT.create()
                .withIssuer(decodedJWT.getIssuer())
                .withIssuedAt(now)
                .withExpiresAt(validity)
                .withClaim("id", decodedJWT.getClaim("id").asString())
                .withClaim("email", decodedJWT.getClaim("email").asString())
                .withClaim("role", decodedJWT.getClaim("role").asString())
                .sign(Algorithm.HMAC256(secretKey));
    }

    public ResponseCookie createJwtCookie(String token) {
        return ResponseCookie.from("JWT", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(10803)
                .build();
    }

    public String getSenderId(String token) {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secretKey)).build();
        DecodedJWT decodedJWT = verifier.verify(token);

        return decodedJWT.getClaim("id").asString();
    }
}
