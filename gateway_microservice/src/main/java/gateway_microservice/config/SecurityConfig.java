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
            .authorizeExchange();

        // Configure public routes
        routeConfig.getPublicRoutes().forEach((service, routes) -> {
            if (routes != null) {
                routes.forEach(route -> authorizeExchange.pathMatchers(route).permitAll());
            }
        });

        // Configure protected routes
        routeConfig.getProtectedRoutes().forEach((service, roleBasedRoutes) -> {
            if (roleBasedRoutes != null) {
                roleBasedRoutes.forEach((role, routes) -> {
                    if ("ADMIN".equals(role)) {
                        routes.forEach(route -> authorizeExchange.pathMatchers(route).hasRole("ADMIN"));
                    } else if ("USER".equals(role)) {
                        routes.forEach(route -> authorizeExchange.pathMatchers(route).hasAnyRole("USER", "ADMIN"));
                    }
                });
            }
        });

        authorizeExchange.anyExchange().permitAll()
            .and()
            .csrf().disable()  // Updated way to disable CSRF protection
            .authenticationManager(authenticationManager)
            .securityContextRepository(securityContextRepository);

        return http.build();
    }
}
