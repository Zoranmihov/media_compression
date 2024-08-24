package gateway_microservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
public class BlacklistService {

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public Mono<Boolean> isBlacklisted(String key) {
        if (key == null) {
            return Mono.error(new IllegalArgumentException("Key must not be null"));
        }
        return reactiveRedisTemplate.hasKey(key)
                .flatMap(exists -> Mono.just(exists))
                .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error checking blacklist status")));
    }
}
