package com.flowforge;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
* @Container on a static field = ONE Redis container shared across every
 * test class extending this base. It starts once for the whole test run,
 * not once per test method — keeps the suite fast.
 *
 * @DynamicPropertySource overrides spring.data.redis.host/port at test
 * runtime to point at the Testcontainers-managed instance instead of
 * whatever's in application.yml. Zero config file changes needed for tests.
 * */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
public abstract class BaseRedisIntegrationTest {

    @Container
    static final RedisContainer REDIS =
            new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }


}
