package com.bbluxurycars.backend.web;

import com.bbluxurycars.backend.error.ApiException;
import com.bbluxurycars.backend.security.FirebaseAuthFilter;
import com.bbluxurycars.backend.sync.TenantMirrorService;
import com.bbluxurycars.backend.sync.TenantMirrorService.MirrorSummary;
import com.bbluxurycars.backend.tenant.TenantContext;
import com.bbluxurycars.backend.tenant.TenantContextService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Forces a full mirror of the caller's own tenant into Postgres.
 *
 * <p>Exists because lazy per-user provisioning only mirrors people who have
 * signed in since the mirror was deployed. An agency's fleet has to be present
 * before its renters can book through the API, and the initial migration needs
 * a way to pull an entire tenant across in one go.
 *
 * <p>Restricted to the tenant's own active admin, and it takes no tenant
 * parameter: the company synced is always the caller's. An operator-wide
 * "sync any tenant" belongs with the platform superadmin role, which does not
 * exist yet (docs/SAAS_ROADMAP.md 3.5).
 */
@RestController
public class SyncController {

    private final TenantContextService tenantContextService;
    private final TenantMirrorService tenantMirrorService;

    public SyncController(TenantContextService tenantContextService,
                          TenantMirrorService tenantMirrorService) {
        this.tenantContextService = tenantContextService;
        this.tenantMirrorService = tenantMirrorService;
    }

    public record SyncResponse(String companyId, boolean companyMirrored,
                               int usersMirrored, int carsMirrored) {
    }

    @PostMapping("/v1/tenant/sync")
    public SyncResponse sync(HttpServletRequest request) {
        String uid = (String) request.getAttribute(FirebaseAuthFilter.UID_ATTRIBUTE);
        TenantContext context = tenantContextService.resolve(uid);
        if (!context.isActiveAdmin() || !context.hasTenantScope()) {
            throw ApiException.forbidden("sync_not_permitted",
                    "Only an active agency admin may sync a tenant");
        }

        MirrorSummary summary = tenantMirrorService.mirrorTenant(context.companyId());
        return new SyncResponse(
                context.companyId(),
                summary.companyMirrored(),
                summary.usersMirrored(),
                summary.carsMirrored());
    }
}
