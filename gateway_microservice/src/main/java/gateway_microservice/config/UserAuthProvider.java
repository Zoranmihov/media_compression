package gateway_microservice.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import gateway_microservice.dtos.UserDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@Component
public class UserAuthProvider {

    @Value("${jwt_secret:default-secret-key}")
    private String secretKey;

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public Mono<Authentication> validateTokenAsync(String token) {
        try {
            Algorithm algo = Algorithm.HMAC256(secretKey);
            JWTVerifier verifier = JWT.require(algo).build();
            DecodedJWT decodedJWT = verifier.verify(token);

            UserDTO user = UserDTO.builder()
                .username(decodedJWT.getIssuer())
                .email(decodedJWT.getClaim("email").asString())
                .displayName(decodedJWT.getClaim("displayName").asString())
                .role(decodedJWT.getClaim("role").asString())
                .build();

            return Mono.just(new UsernamePasswordAuthenticationToken(user, token, Collections.singletonList(() -> "ROLE_" + user.getRole())));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public String refreshToken(String oldToken) {
        Algorithm algo = Algorithm.HMAC256(secretKey);
        JWTVerifier verifier = JWT.require(algo).build();
        DecodedJWT decodedJWT = verifier.verify(oldToken);

        Date now = new Date();
        Date expiresAt = decodedJWT.getExpiresAt();

        long timeToExpiry = expiresAt.getTime() - now.getTime();
        if (timeToExpiry > 20 * 60 * 1000) {
            return oldToken;
        }

        String username = decodedJWT.getIssuer();
        String email = decodedJWT.getClaim("email").asString();
        String displayName = decodedJWT.getClaim("displayName").asString();
        String role = decodedJWT.getClaim("role").asString();

        Date validity = new Date(now.getTime() + 10800000);

        return JWT.create()
                .withIssuer(username)
                .withIssuedAt(now)
                .withExpiresAt(validity)
                .withClaim("email", email)
                .withClaim("displayName", displayName)
                .withClaim("role", role)
                .sign(algo);
    }

    public ResponseCookie createJwtCookie(String token) {
        return ResponseCookie.from("JWT", token)
                .httpOnly(true)
                .secure(true) // Set to true if you're using HTTPS
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // Set the expiry time as per your requirement
                .build();
    }
}
