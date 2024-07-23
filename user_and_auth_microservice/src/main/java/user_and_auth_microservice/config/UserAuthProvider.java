package user_and_auth_microservice.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import user_and_auth_microservice.dtos.UserDTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;


@RequiredArgsConstructor
@Component
public class UserAuthProvider {

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
                .withClaim("email", userDTO.getEmail())
                .withClaim("displayName", userDTO.getDisplayName())
                .withClaim("role", userDTO.getRole())
                .sign(Algorithm.HMAC256(secretKey));
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

}
