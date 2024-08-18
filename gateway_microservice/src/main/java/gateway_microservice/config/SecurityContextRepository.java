package gateway_microservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import gateway_microservice.service.UserAuthProviderService;
import reactor.core.publisher.Mono;

@Component
public class SecurityContextRepository implements ServerSecurityContextRepository {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserAuthProviderService userAuthProvider;

    @Override
    public Mono<Void> save(ServerWebExchange swe, SecurityContext sc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
public Mono<SecurityContext> load(ServerWebExchange swe) {
    String authHeader = swe.getRequest().getHeaders().getFirst("Authorization");
    Mono<Authentication> authMono;

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String authToken = authHeader.substring(7);
        authMono = checkAndValidateToken(swe, authToken);
    } else {
        authMono = Mono.justOrEmpty(swe.getRequest().getCookies().getFirst("JWT"))
                .map(cookie -> cookie.getValue())
                .flatMap(token -> {
                    return checkAndValidateToken(swe, token);
                });
    }

    return authMono.flatMap(auth -> this.authenticationManager.authenticate(auth)
            .map(SecurityContextImpl::new));
}


private Mono<Authentication> checkAndValidateToken(ServerWebExchange swe, String authToken) {
    return userAuthProvider.validateTokenAsync(authToken).flatMap(auth -> {
        String refreshedToken = userAuthProvider.refreshToken(auth.getCredentials().toString());
        if (!auth.getCredentials().toString().equals(refreshedToken)) {
            swe.getResponse().addCookie(userAuthProvider.createJwtCookie(refreshedToken));
            swe.getResponse().mutate().header("X-Token-Refreshed", refreshedToken).build();
            swe.getRequest().mutate().header("Authorization", "Bearer " + refreshedToken).build();
        }

        // Use the getSenderId method to extract the user ID and attach it as a header
        String userId = userAuthProvider.getSenderId(authToken);
        swe.getRequest().mutate().header("UserId", userId).build();

        return Mono.just(auth);
    });
}

}
