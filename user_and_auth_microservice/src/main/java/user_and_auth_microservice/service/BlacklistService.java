package user_and_auth_microservice.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BlacklistService {

    private static final String BLACKLISTED_VALUE = "blacklisted";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void blacklistByLogin(String login) {
        try {
            redisTemplate.opsForValue().set(login, BLACKLISTED_VALUE, 3, TimeUnit.HOURS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
