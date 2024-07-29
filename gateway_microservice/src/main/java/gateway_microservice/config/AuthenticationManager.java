package gateway_microservice.config;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import gateway_microservice.service.UserAuthProviderService;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationManager implements ReactiveAuthenticationManager {

    private final UserAuthProviderService userAuthProvider;

    public AuthenticationManager(UserAuthProviderService userAuthProvider) {
        this.userAuthProvider = userAuthProvider;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();
        return userAuthProvider.validateTokenAsync(token)
                .onErrorResume(e -> Mono.empty());
    }
}
