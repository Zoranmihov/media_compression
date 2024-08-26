package gateway_microservice.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;


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

        // TODO remove for production 
        authorizeExchange.pathMatchers(HttpMethod.OPTIONS).permitAll();

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
            .csrf().disable()
            .authenticationManager(authenticationManager)
            .securityContextRepository(securityContextRepository);

        return http.build();
    }

    @Bean
    public CorsWebFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.asList("http://localhost:8083")); // Frontend origin
        config.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
