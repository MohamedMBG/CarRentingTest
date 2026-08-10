package com.bbluxurycars.backend.web;

import com.bbluxurycars.backend.domain.AppUser;
import com.bbluxurycars.backend.domain.Company;
import com.bbluxurycars.backend.domain.CompanyLifecycleStatus;
import com.bbluxurycars.backend.domain.UserLifecycleStatus;
import com.bbluxurycars.backend.domain.UserRole;
import com.bbluxurycars.backend.domain.VerificationStatus;
import com.bbluxurycars.backend.repository.AppUserRepository;
import com.bbluxurycars.backend.repository.CompanyRepository;
import com.bbluxurycars.backend.security.FirebaseAuthFilter;
import com.bbluxurycars.backend.support.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the wire contract of {@code GET /v1/me} against real data.
 *
 * <p>Built with {@code standaloneSetup} rather than the full filter chain so
 * that {@link FirebaseAuthFilter} is not in the way: verifying a genuine
 * Firebase ID token would need real credentials, and the filter's own
 * behaviour is a separate concern from the payload this controller produces.
 * The uid is injected as the request attribute exactly as the filter sets it.
 */
@Transactional
class MeControllerTest extends AbstractPostgresIntegrationTest {

    private static final String TENANT = "company-me";

    @Autowired
    private MeController meController;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(meController).build();
    }

    @Test
    void returnsTenantRoleAndPermissionsForAProvisionedClient() throws Exception {
        Company company = new Company(TENANT, "Agency Me");
        company.setStatus(CompanyLifecycleStatus.APPROVED);
        companyRepository.save(company);

        AppUser user = new AppUser("uid-me-client", UserRole.CLIENT);
        user.setCompanyId(TENANT);
        user.setStatus(UserLifecycleStatus.ACTIVE);
        user.setVerificationStatus(VerificationStatus.APPROVED);
        appUserRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/v1/me")
                        .requestAttr(FirebaseAuthFilter.UID_ATTRIBUTE, "uid-me-client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("uid-me-client"))
                .andExpect(jsonPath("$.provisioned").value(true))
                .andExpect(jsonPath("$.companyId").value(TENANT))
                // Statuses go out as the same lowercase strings Firestore holds
                // so the Android client can reuse its existing parsers.
                .andExpect(jsonPath("$.role").value("client"))
                .andExpect(jsonPath("$.userStatus").value("active"))
                .andExpect(jsonPath("$.companyStatus").value("approved"))
                .andExpect(jsonPath("$.verificationStatus").value("approved"))
                .andExpect(jsonPath("$.permissions.canBook").value(true))
                .andExpect(jsonPath("$.permissions.isActiveAdmin").value(false));
    }

    @Test
    void reportsUnprovisionedRatherThanFailingWhenNoMirrorRowExists() throws Exception {
        mockMvc.perform(get("/v1/me")
                        .requestAttr(FirebaseAuthFilter.UID_ATTRIBUTE, "uid-not-mirrored"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("uid-not-mirrored"))
                .andExpect(jsonPath("$.provisioned").value(false))
                // Present but null: the key stays in the payload so clients can
                // distinguish "no tenant" from an older server that never sent
                // the field at all.
                .andExpect(jsonPath("$.companyId").value(nullValue()))
                .andExpect(jsonPath("$.role").value("unknown"))
                .andExpect(jsonPath("$.permissions.canBook").value(false))
                .andExpect(jsonPath("$.permissions.isActiveAdmin").value(false));
    }
}
