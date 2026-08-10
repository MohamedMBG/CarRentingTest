package com.bbluxurycars.backend.tenant;

import com.bbluxurycars.backend.domain.AppUser;
import com.bbluxurycars.backend.domain.Company;
import com.bbluxurycars.backend.domain.CompanyLifecycleStatus;
import com.bbluxurycars.backend.repository.AppUserRepository;
import com.bbluxurycars.backend.repository.CompanyRepository;
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

    public TenantContextService(AppUserRepository appUserRepository,
                                CompanyRepository companyRepository) {
        this.appUserRepository = appUserRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public TenantContext resolve(String uid) {
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("uid is required");
        }

        Optional<AppUser> found = appUserRepository.findByFirebaseUid(uid);
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
