package com.bbluxurycars.backend.web;

import com.bbluxurycars.backend.privacy.PrivacyService;
import com.bbluxurycars.backend.security.FirebaseAuthFilter;
import com.bbluxurycars.backend.web.dto.DeleteAccountResponse;
import com.bbluxurycars.backend.web.dto.UserExportResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GDPR data-rights endpoints: {@code POST /v1/user/export} and
 * {@code POST /v1/user/delete}. Closes docs/SAAS_ROADMAP.md 1.3/3.10 -- the
 * Android privacy centre already presents both controls; this is what makes
 * them do something.
 *
 * <p>Neither route takes a uid or email in the body -- {@code DataRightsService}
 * on the client sends one, but it is ignored. The verified uid from
 * {@link FirebaseAuthFilter} is the only identity either endpoint acts on, so
 * no caller can export or erase anyone but themselves.
 */
@RestController
public class PrivacyController {

    private final PrivacyService privacyService;

    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @PostMapping("/v1/user/export")
    public UserExportResponse export(HttpServletRequest request) {
        return privacyService.exportData(uidOf(request));
    }

    @PostMapping("/v1/user/delete")
    public DeleteAccountResponse delete(HttpServletRequest request) {
        return privacyService.deleteData(uidOf(request));
    }

    private static String uidOf(HttpServletRequest request) {
        return (String) request.getAttribute(FirebaseAuthFilter.UID_ATTRIBUTE);
    }
}
