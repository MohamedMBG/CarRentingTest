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
import com.bbluxurycars.backend.support.FirestoreTestConfig;
import com.bbluxurycars.backend.support.InMemoryFirestoreEraser;
import com.bbluxurycars.backend.support.InMemoryFirestoreGateway;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The wire contract of {@code POST /v1/user/export} and
 * {@code POST /v1/user/delete}, and the one invariant that matters most for
 * both: the uid acted on is always the verified one from
 * {@link FirebaseAuthFilter}, never anything a request body could name.
 */
@Transactional
@Import(FirestoreTestConfig.class)
class PrivacyControllerTest extends AbstractPostgresIntegrationTest {

    private static final String TENANT = "company-privacy";
    private static final String UID = "uid-privacy-renter";

    @Autowired
    private PrivacyController privacyController;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private InMemoryFirestoreGateway firestoreGateway;

    @Autowired
    private InMemoryFirestoreEraser firestoreEraser;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(privacyController).build();

        firestoreGateway.clear();
        firestoreEraser.clear();

        Company company = new Company(TENANT, "Agency Privacy");
        company.setStatus(CompanyLifecycleStatus.APPROVED);
        companyRepository.save(company);

        AppUser user = new AppUser(UID, UserRole.CLIENT);
        user.setCompanyId(TENANT);
        user.setEmail("renter@example.com");
        user.setFullName("Renter Example");
        user.setStatus(UserLifecycleStatus.ACTIVE);
        user.setVerificationStatus(VerificationStatus.APPROVED);
        appUserRepository.save(user);

        firestoreGateway.put("users", UID, Map.of("name", "Renter Example", "companyId", TENANT));
        firestoreGateway.put("verification_requests", UID, Map.of("status", "approved", "companyId", TENANT));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void exportsTheCallersOwnDataFromBothStores() throws Exception {
        mockMvc.perform(post("/v1/user/export")
                        .requestAttr(FirebaseAuthFilter.UID_ATTRIBUTE, UID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value(UID))
                .andExpect(jsonPath("$.account.email").value("renter@example.com"))
                .andExpect(jsonPath("$.firestoreProfile.name").value("Renter Example"))
                .andExpect(jsonPath("$.firestoreVerification.status").value("approved"));
    }

    /**
     * The client's {@code DataRightsService} sends its own {@code uid} field in
     * the body; this proves it is ignored in favour of the verified attribute.
     */
    @Test
    void exportIgnoresAnyUidTheRequestBodyClaims() throws Exception {
        mockMvc.perform(post("/v1/user/export")
                        .requestAttr(FirebaseAuthFilter.UID_ATTRIBUTE, UID)
                        .content("{\"uid\":\"someone-elses-uid\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value(UID));
    }

    @Test
    void deletionAnonymisesPostgresAndErasesFirestoreStorageAndAuth() throws Exception {
        mockMvc.perform(post("/v1/user/delete")
                        .requestAttr(FirebaseAuthFilter.UID_ATTRIBUTE, UID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        entityManager.flush();
        entityManager.clear();

        AppUser anonymised = appUserRepository.findByFirebaseUid(UID).orElseThrow();
        assertThat(anonymised.getEmail()).isNull();
        assertThat(anonymised.getFullName()).isNull();
        // The row itself survives: rental_request rows in the same tenant
        // carry a foreign key to it, and it is the agency's ledger, not the
        // renter's personal data.
        assertThat(anonymised.getId()).isEqualTo(UID);

        assertThat(firestoreEraser.deletedDocuments)
                .contains("users/" + UID, "verification_requests/" + UID);
        assertThat(firestoreEraser.deletedCollections)
                .contains("verification_requests/" + UID + "/evidence");
        assertThat(firestoreEraser.deletedStoragePrefixes)
                .contains("verification_evidence/" + UID + "/");
        assertThat(firestoreEraser.deletedAuthUsers).contains(UID);
    }
}
