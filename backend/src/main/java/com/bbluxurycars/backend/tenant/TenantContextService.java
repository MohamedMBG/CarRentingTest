package com.bbluxurycars.backend.tenant;

import com.bbluxurycars.backend.domain.AppUser;
import com.bbluxurycars.backend.domain.Company;
import com.bbluxurycars.backend.domain.CompanyLifecycleStatus;
import com.bbluxurycars.backend.repository.AppUserRepository;
import com.bbluxurycars.backend.repository.CompanyRepository;
import com.bbluxurycars.backend.sync.TenantMirrorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Resolves a verified Firebase uid into a {@link TenantContext}.
 *
 * <p>This is the single place the backend decides which tenant a caller belongs
 * to. Everything that later enforces isolation depends on it, so it takes only
 * a uid that {@code FirebaseAuthFilter} has already verified -- never a tenant
 * identifier supplied by the client, which would let a caller simply ask to be
 * in someone else's tenant.
 */
@Service
public class TenantContextService {

    private final AppUserRepository appUserRepository;
    private final CompanyRepository companyRepository;
    private final TenantMirrorService tenantMirrorService;

    public TenantContextService(AppUserRepository appUserRepository,
                                CompanyRepository companyRepository,
                                TenantMirrorService tenantMirrorService) {
        this.appUserRepository = appUserRepository;
        this.companyRepository = companyRepository;
        this.tenantMirrorService = tenantMirrorService;
    }

    /**
     * Read-write rather than read-only because a miss now provisions the caller
     * from Firestore before answering (see {@link #provisionFromFirestore}).
     */
    @Transactional
    public TenantContext resolve(String uid) {
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("uid is required");
        }

        Optional<AppUser> found = appUserRepository.findByFirebaseUid(uid)
                .or(() -> provisionFromFirestore(uid));
        if (found.isEmpty()) {
            return TenantContext.unprovisioned(uid);
        }

        AppUser user = found.get();
        return new TenantContext(
                user.getId(),
                true,
                user.getCompanyId(),
                user.getRole(),
                user.getStatus(),
                resolveCompanyStatus(user),
                user.getVerificationStatus());
    }

    /**
     * Mirrors the caller from Firestore on their first authenticated request.
     *
     * <p>Provisioning lazily rather than waiting for a bulk backfill means a
     * user reaches the API the moment they use it, and the tenant fields on
     * {@code /v1/me} stop being empty for everyone until an operator runs
     * something. The bulk path still exists for the initial migration.
     *
     * <p>A failure here is deliberately silent: the caller stays unprovisioned,
     * which is a supported state the client already handles by staying on its
     * Firestore path. Turning a Firestore outage into a 500 on every request
     * would take a working app offline for a mirror it does not yet depend on.
     */
    private Optional<AppUser> provisionFromFirestore(String uid) {
        return tenantMirrorService.mirrorUser(uid);
    }

    /**
     * A user with no company resolves to
     * {@link CompanyLifecycleStatus#PENDING_REVIEW}, matching the default the
     * client applies when the company document is absent.
     *
     * <p>The {@code orElse} branch below is defence in depth, not a live path:
     * the foreign key on {@code app_user.company_id} means a user cannot
     * reference a company that is not mirrored, so a dangling tenant reference
     * cannot reach this code. It stays because the failure it guards against --
     * treating an unknown tenant as approved -- grants access rather than
     * withholding it, and that is the wrong way to fail if the constraint is
     * ever relaxed.
     */
    private CompanyLifecycleStatus resolveCompanyStatus(AppUser user) {
        if (!user.hasTenantScope()) {
            return CompanyLifecycleStatus.PENDING_REVIEW;
        }
        return companyRepository.findById(user.getCompanyId())
                .map(Company::getStatus)
                .orElse(CompanyLifecycleStatus.PENDING_REVIEW);
    }
}
