package com.flowforge;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for Redis integration tests — Singleton Container Pattern.
 *
 * The container starts ONCE, in this static initializer, for the entire
 * JVM/test run — not per test class. This is deliberate: JUnit's
 * @Testcontainers + @Container lifecycle manages containers PER TEST CLASS
 * (start before that class's first test, stop after its last), even for a
 * static field shared across subclasses. Combined with Spring's test
 * context caching (which reuses an ApplicationContext across test classes
 * when the configuration looks identical), that per-class stop/restart
 * cycle leaves a cached context wired to a port whose container no longer
 * exists — exactly the "Connection refused" failure you hit.
 *
 * Starting once and never stopping (Ryuk cleans it up at JVM exit) keeps
 * the port stable for the whole suite, so the cached context stays valid
 * across every test class that extends this base.
 *
 * @DynamicPropertySource overrides spring.data.redis.host/port at test
 * runtime to point at the Testcontainers-managed instance instead of
 * whatever's in application.yml. Zero config file changes needed for tests.
 * */

@SpringBootTest(
        classes = FlowForgeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)

public abstract class BaseRedisIntegrationTest {


    static final RedisContainer REDIS ;
    static {
        REDIS = new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));
        REDIS.start();
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        // No .stop() call — intentional. See class-level comment.
    }


}
