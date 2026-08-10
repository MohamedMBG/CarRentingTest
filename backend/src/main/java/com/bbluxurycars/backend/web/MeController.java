package com.bbluxurycars.backend.web;

import com.bbluxurycars.backend.security.FirebaseAuthFilter;
import com.bbluxurycars.backend.tenant.TenantContext;
import com.bbluxurycars.backend.tenant.TenantContextService;
import com.bbluxurycars.backend.web.dto.MeResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns the caller's identity together with their tenant, role and lifecycle
 * status, replacing the Firestore reads the Android app's
 * {@code TenantSessionProvider} performs on the device.
 *
 * <p>The uid comes from the request attribute set by
 * {@link FirebaseAuthFilter} after verifying the ID token, never from the
 * request body or a path variable -- the client does not get to say who it is.
 *
 * <p>Callers must check {@code provisioned} before trusting the tenant fields:
 * until the Firestore backfill lands most users have no mirrored row, and an
 * unprovisioned context reports the least-privileged value for every status
 * (docs/SAAS_ROADMAP.md).
 */
@RestController
public class MeController {

    private final TenantContextService tenantContextService;

    public MeController(TenantContextService tenantContextService) {
        this.tenantContextService = tenantContextService;
    }

    @GetMapping("/v1/me")
    public MeResponse me(HttpServletRequest request) {
        String uid = (String) request.getAttribute(FirebaseAuthFilter.UID_ATTRIBUTE);
        TenantContext context = tenantContextService.resolve(uid);
        return MeResponse.from(context);
    }
}
