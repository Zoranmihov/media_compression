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
    private final Map<String, List<String>> protectedRoutes = new HashMap<>();

    @PostConstruct
    public void init() {
        Yaml yaml = new Yaml();

        try {
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    Map<String, List<String>> routes = yaml.load(in);
                    String serviceName = resource.getFilename().replace(".yml", "");
                    publicRoutes.put(serviceName, routes.get("public"));
                    protectedRoutes.put(serviceName, routes.get("protected"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, List<String>> getPublicRoutes() {
        return publicRoutes;
    }

    public Map<String, List<String>> getProtectedRoutes() {
        return protectedRoutes;
    }
}
