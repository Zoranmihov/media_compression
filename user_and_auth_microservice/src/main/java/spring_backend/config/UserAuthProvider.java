package spring_backend.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import spring_backend.dtos.UserDTO;

import java.util.Base64;
import java.util.Collections;
import java.util.Date;


@RequiredArgsConstructor
@Component
public class UserAuthProvider {

    @Value("${{security.jwt.token.secret-key:secret-key}}")
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
                .withClaim("email", userDTO.getEmail())
                .withClaim("displayName", userDTO.getDisplayName())
                .sign(Algorithm.HMAC256(secretKey));
    }

    public Authentication validateToken(String token){
        Algorithm algo = Algorithm.HMAC256(secretKey);

        JWTVerifier verifier = JWT.require(algo).build();

        DecodedJWT decodedJWT = verifier.verify(token);

       UserDTO user = UserDTO.builder()
                .username(decodedJWT.getIssuer())
                .email(decodedJWT.getClaim("email").asString())
                .displayName(decodedJWT.getClaim("displayName").asString())
                .build();

        return new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
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

        Date validity = new Date(now.getTime() + 10800000);

        return JWT.create()
                .withIssuer(username)
                .withIssuedAt(now)
                .withExpiresAt(validity)
                .withClaim("email", email)
                .withClaim("displayName", displayName)
                .sign(algo);
    }

}
