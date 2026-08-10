package com.bbluxurycars.backend.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for tests that need the real schema.
 *
 * <p>The container is static, so one Postgres is started for the whole test JVM
 * and reused across every subclass rather than paid for per class. Flyway runs
 * against it on first context load, which means the migrations themselves are
 * exercised by every integration test -- a broken migration fails the build
 * here rather than on deploy.
 *
 * <p>{@code @ServiceConnection} wires the container's JDBC URL, username and
 * password into the context automatically, so no test needs to restate the
 * datasource properties.
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
