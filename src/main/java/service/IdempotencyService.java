package service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;


public class IdempotencyService {
    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate stringRedisTemplate){
        this.redisTemplate = stringRedisTemplate;
    }

    public boolean tryAcquire(String key) {
        boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, "PROCESSING", Duration.ofHours(24));

        return Boolean.TRUE.equals(success);
    }

    public void markSuccess(String key) {
        redisTemplate.opsForValue()
                .set(key, "SUCCESS", Duration.ofHours(24));
    }

    public boolean isCompleted(String key) {
        String status = redisTemplate.opsForValue().get(key);
        return "SUCCESS".equals(status);
    }
}
