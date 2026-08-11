package com.bbluxurycars.backend.config;

import com.bbluxurycars.backend.security.FirebaseAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    // Scoped explicitly to authenticated routes (starting with /v1/me) rather
    // than all of /v1/* — /v1/health stays public, and future public routes
    // (e.g. a Stripe webhook, which authenticates differently) won't be
    // silently caught by a broad pattern as more endpoints are added.
    @Bean
    public FilterRegistrationBean<FirebaseAuthFilter> firebaseAuthFilter() {
        FilterRegistrationBean<FirebaseAuthFilter> registration = new FilterRegistrationBean<>(new FirebaseAuthFilter());
        registration.addUrlPatterns(
                "/v1/me", "/v1/me/*",
                // Fleet and bookings are tenant data: the tenant is resolved
                // from the verified uid, so neither is reachable without the
                // filter having run.
                "/v1/cars", "/v1/cars/*",
                "/v1/bookings", "/v1/bookings/*");
        return registration;
    }
}
