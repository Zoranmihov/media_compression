package gateway_microservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Configuration
public class RouteConfig {

    @Value("classpath:routes-config/*.yml")
    private Resource[] resources;

    private final Map<String, List<String>> publicRoutes = new HashMap<>();
    private final Map<String, Map<String, List<String>>> protectedRoutes = new HashMap<>();

    @PostConstruct
    public void init() {
        Yaml yaml = new Yaml();

        try {
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    Map<String, Object> routes = yaml.load(in);
                    String serviceName = resource.getFilename().replace(".yml", "");

                    // Safely cast the public routes
                    Object publicRoutesObj = routes.get("public");
                    if (publicRoutesObj instanceof List) {
                        publicRoutes.put(serviceName, castToListOfString(publicRoutesObj));
                    } else {
                        System.err.println("Invalid type for public routes in " + serviceName);
                    }

                    // Safely cast the protected routes
                    Object protectedRoutesObj = routes.get("protected");
                    if (protectedRoutesObj instanceof Map) {
                        Map<String, List<String>> roleBasedRoutes = new HashMap<>();
                        Map<?, ?> tempMap = (Map<?, ?>) protectedRoutesObj;
                        for (Map.Entry<?, ?> entry : tempMap.entrySet()) {
                            if (entry.getKey() instanceof String && entry.getValue() instanceof List) {
                                roleBasedRoutes.put((String) entry.getKey(), castToListOfString(entry.getValue()));
                            }
                        }
                        protectedRoutes.put(serviceName, roleBasedRoutes);
                    } else {
                        System.err.println("Invalid type for protected routes in " + serviceName);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> castToListOfString(Object obj) {
        try {
            return (List<String>) obj;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid type, expected List<String>", e);
        }
    }

    public Map<String, List<String>> getPublicRoutes() {
        return publicRoutes;
    }

    public Map<String, Map<String, List<String>>> getProtectedRoutes() {
        return protectedRoutes;
    }
}
