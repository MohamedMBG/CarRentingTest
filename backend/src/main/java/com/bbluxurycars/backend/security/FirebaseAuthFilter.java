package com.bbluxurycars.backend.security;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Verifies the Firebase ID token on the Authorization header and exposes the
 * caller's uid as a request attribute. This is the first piece of the
 * server-side authorization boundary described in docs/SAAS_ROADMAP.md
 * (Phase 1). The uid it sets is the only identity downstream code may trust:
 * TenantContextService resolves the tenant from it, and never from anything
 * the client sends.
 *
 * Not a @Component: registered explicitly (and URL-scoped) by WebConfig so
 * Spring Boot doesn't also auto-register it as a global /* filter.
 */
public class FirebaseAuthFilter extends OncePerRequestFilter {

    public static final String UID_ATTRIBUTE = "firebaseUid";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing bearer token");
            return;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Firebase Admin SDK not configured on this instance");
            return;
        }
        String idToken = header.substring("Bearer ".length());
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            request.setAttribute(UID_ATTRIBUTE, decoded.getUid());
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Firebase ID token");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
