package gateway_microservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityContextRepository implements ServerSecurityContextRepository {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserAuthProvider userAuthProvider;

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
            authMono = userAuthProvider.validateTokenAsync(authToken);
        } else {
            authMono = Mono.justOrEmpty(swe.getRequest().getCookies().getFirst("JWT"))
                    .map(cookie -> cookie.getValue())
                    .flatMap(userAuthProvider::validateTokenAsync);
        }

        return authMono.flatMap(auth -> {
            String refreshedToken = userAuthProvider.refreshToken(auth.getCredentials().toString());
            if (!auth.getCredentials().toString().equals(refreshedToken)) {
                swe.getResponse().addCookie(userAuthProvider.createJwtCookie(refreshedToken));
            }
            return this.authenticationManager.authenticate(auth).map(SecurityContextImpl::new);
        });
    }
}
