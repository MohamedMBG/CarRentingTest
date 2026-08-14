package com.bbluxurycars.backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the Firebase-backed gateway with the in-memory one for tests that
 * exercise the mirror.
 *
 * <p>{@code @Primary} rather than excluding the real bean: the real gateway
 * stays in the context, so a wiring mistake in it still fails the build.
 */
@TestConfiguration
public class FirestoreTestConfig {

    @Bean
    @Primary
    public InMemoryFirestoreGateway inMemoryFirestoreGateway() {
        return new InMemoryFirestoreGateway();
    }

    @Bean
    @Primary
    public InMemoryFirestoreEraser inMemoryFirestoreEraser() {
        return new InMemoryFirestoreEraser();
    }
}
