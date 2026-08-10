package com.bbluxurycars.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for tests that need the real schema.
 *
 * <p>Uses the singleton-container pattern: the container is started once from a
 * static initializer and deliberately never stopped. The obvious alternative --
 * {@code @Testcontainers} with a {@code @Container} static field -- looks
 * equivalent but is not, because that extension stops the container when each
 * test class finishes. With the field inherited from a shared base, the first
 * class to run would shut down the very container every later class expects to
 * still be listening, and those classes fail with connection refused.
 *
 * <p>Nothing leaks: Testcontainers' Ryuk sidecar removes the container when the
 * test JVM exits. Starting once also means one Postgres for the whole suite
 * rather than one per class.
 *
 * <p>Flyway runs against it on first context load, so the migrations themselves
 * are exercised by every integration test -- a broken migration fails the build
 * here rather than on deploy.
 */
@SpringBootTest
public abstract class AbstractPostgresIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    /**
     * Registered dynamically because the mapped port is only known after the
     * container starts, so it cannot be written into a properties file.
     */
    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
