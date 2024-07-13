package gateway_microservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final RouteConfig routeConfig;

    public SecurityConfig(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository, RouteConfig routeConfig) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.routeConfig = routeConfig;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        ServerHttpSecurity.AuthorizeExchangeSpec authorizeExchange = http
            .csrf().disable()
            .authorizeExchange();

        // Dynamically configure routes
        routeConfig.getProtectedRoutes().forEach((service, routes) -> {
            routes.forEach(route -> authorizeExchange.pathMatchers(route).authenticated());
        });

        routeConfig.getPublicRoutes().forEach((service, routes) -> {
            routes.forEach(route -> authorizeExchange.pathMatchers(route).permitAll());
        });

        authorizeExchange.anyExchange().permitAll()
            .and()
            .authenticationManager(authenticationManager)
            .securityContextRepository(securityContextRepository);

        return http.build();
    }
}
