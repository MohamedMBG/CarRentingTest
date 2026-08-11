package com.bbluxurycars.backend.web;

import com.bbluxurycars.backend.security.FirebaseAuthFilter;
import com.bbluxurycars.backend.support.AbstractPostgresIntegrationTest;
import com.bbluxurycars.backend.support.FirestoreTestConfig;
import com.bbluxurycars.backend.support.InMemoryFirestoreGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /v1/tenant/sync}: who may run a backfill, and what it reports.
 *
 * <p>Both callers are provisioned lazily from the stand-in Firestore during the
 * request, which also exercises that path end to end through a controller.
 */
@Transactional
@Import(FirestoreTestConfig.class)
class SyncControllerTest extends AbstractPostgresIntegrationTest {

    private static final String TENANT = "company-sync";

    @Autowired
    private SyncController syncController;

    @Autowired
    private InMemoryFirestoreGateway firestore;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(syncController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        firestore.clear();
        firestore.put("companies", TENANT, Map.of("name", "Sync Motors", "status", "approved"));
        firestore.put("users", "uid-sync-admin", Map.of(
                "name", "Admin", "role", "admin", "companyId", TENANT, "status", "active"));
        firestore.put("users", "uid-sync-client", Map.of(
                "name", "Client", "role", "client", "companyId", TENANT,
                "status", "active", "verification_status", "approved"));
        firestore.put("cars", "car-sync-1", Map.of(
                "model", "Clio", "pricePerDay", 300.0, "available", true, "companyId", TENANT));
    }

    @Test
    void anActiveAdminSyncsTheirOwnTenant() throws Exception {
        mockMvc.perform(post("/v1/tenant/sync")
                        .requestAttr(FirebaseAuthFilter.UID_ATTRIBUTE, "uid-sync-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(TENANT))
                .andExpect(jsonPath("$.companyMirrored").value(true))
                .andExpect(jsonPath("$.usersMirrored").value(2))
                .andExpect(jsonPath("$.carsMirrored").value(1));
    }

    /**
     * A renter cannot pull their agency's whole user list into Postgres, even
     * though the sync only ever touches their own tenant.
     */
    @Test
    void aRenterMayNotSyncTheTenant() throws Exception {
        mockMvc.perform(post("/v1/tenant/sync")
                        .requestAttr(FirebaseAuthFilter.UID_ATTRIBUTE, "uid-sync-client"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("sync_not_permitted"));
    }
}
