package com.flowforge.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RedisConfig {


    /**
     *  RedisTemplate with String serializers for both key and value
     *  Spring Boot's autoconfigured RedisTemplate<Object, Object> uses
     *  JdkSerializationRedisSerializer — writes binary data.
     *
     *  StringRedisSerializer keeps everything human-readable:
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory){
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);     // hash field names
        template.setHashValueSerializer(serializer);   // hash field values
        template.afterPropertiesSet();

        return template;

    }



/**
 * Pre-loads the rate limiting Lua script at startup.
 *
 * HOW SPRING EXECUTES LUA SCRIPTS:
 * 1. First call: sends full script via EVAL, Redis executes and caches it
 *    under a SHA1 hash of the script content
 * 2. Subsequent calls: sends only the SHA1 via EVALSHA — faster,
 *    less network bandwidth
 * 3. If Redis restarts (cache cleared): Spring automatically falls back
 *    to EVAL and re-caches — transparent to application code
 *
 * WHY LUA FOR RATE LIMITING:
 * Redis executes Lua scripts atomically — no other command runs between
 * any two lines of the script. This replaces what would otherwise be a
 * GET → compute → SET sequence that has race conditions between steps.
 */
    @Bean
    public DefaultRedisScript<Long> rateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("scripts/rate_limit.lua"))
        );
        script.setResultType(Long.class);

        return script;
    }
}
