package com.bbluxurycars.backend.web;

import com.bbluxurycars.backend.security.FirebaseAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * First real end-to-end route (Phase 0 in docs/BACKEND_API_PLAN.md): proves
 * the Android app -> backend -> Firebase Admin token verification path
 * works. Returns only the verified uid for now; companyId/role/lifecycle
 * status join against Postgres tenant data lands in Phase 1, replacing the
 * client-side TenantContext.from(...) Firestore reads.
 */
@RestController
public class MeController {

    @GetMapping("/v1/me")
    public Map<String, String> me(HttpServletRequest request) {
        String uid = (String) request.getAttribute(FirebaseAuthFilter.UID_ATTRIBUTE);
        return Map.of("uid", uid);
    }
}
