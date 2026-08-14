package com.bbluxurycars.backend.privacy;

import com.bbluxurycars.backend.domain.AppUser;
import com.bbluxurycars.backend.domain.RentalRequest;
import com.bbluxurycars.backend.firestore.FirestoreDocument;
import com.bbluxurycars.backend.firestore.FirestoreGateway;
import com.bbluxurycars.backend.repository.AppUserRepository;
import com.bbluxurycars.backend.repository.RentalRequestRepository;
import com.bbluxurycars.backend.web.dto.DeleteAccountResponse;
import com.bbluxurycars.backend.web.dto.UserExportResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Backs the two GDPR data-rights endpoints the Android privacy centre already
 * presents to users but that, until now, silently did nothing
 * (docs/SAAS_ROADMAP.md 1.3, 3.10).
 */
@Service
public class PrivacyService {

    // Storage layout owned by FirebaseVerificationService on the client:
    // verification_evidence/{uid}/selfie.jpg and .../license_front.jpg. These
    // are the face image and ID scan the biometric match runs against, which
    // is exactly what "biometric templates" in the roadmap's erasure scope
    // refers to.
    private static final String VERIFICATION_STORAGE_PREFIX = "verification_evidence/";
    private static final String VERIFICATION_REQUESTS_COLLECTION = "verification_requests";
    private static final String USERS_COLLECTION = "users";
    private static final String RENTAL_REQUESTS_COLLECTION = "rental_requests";

    private final AppUserRepository appUserRepository;
    private final RentalRequestRepository rentalRequestRepository;
    private final FirestoreGateway firestoreGateway;
    private final FirestoreEraser firestoreEraser;

    public PrivacyService(AppUserRepository appUserRepository,
                          RentalRequestRepository rentalRequestRepository,
                          FirestoreGateway firestoreGateway,
                          FirestoreEraser firestoreEraser) {
        this.appUserRepository = appUserRepository;
        this.rentalRequestRepository = rentalRequestRepository;
        this.firestoreGateway = firestoreGateway;
        this.firestoreEraser = firestoreEraser;
    }

    @Transactional(readOnly = true)
    public UserExportResponse exportData(String uid) {
        Optional<AppUser> account = appUserRepository.findByFirebaseUid(uid);
        List<Map<String, Object>> bookings = rentalRequestRepository.findAllByUserIdForExport(uid).stream()
                .map(PrivacyService::bookingAsMap)
                .toList();

        Map<String, Object> firestoreProfile = firestoreGateway.findDocument(USERS_COLLECTION, uid)
                .map(FirestoreDocument::data)
                .orElseGet(Map::of);
        Map<String, Object> firestoreVerification = firestoreGateway.findDocument(VERIFICATION_REQUESTS_COLLECTION, uid)
                .map(FirestoreDocument::data)
                .orElseGet(Map::of);
        List<Map<String, Object>> firestoreBookings = firestoreGateway
                .findWhereEquals(RENTAL_REQUESTS_COLLECTION, "userId", uid).stream()
                .map(FirestoreDocument::data)
                .toList();

        return new UserExportResponse(
                uid,
                Instant.now(),
                account.map(PrivacyService::accountAsMap).orElse(null),
                bookings,
                firestoreProfile,
                firestoreVerification,
                firestoreBookings);
    }

    /**
     * Erasure, not row deletion, on two of the three stores it touches.
     *
     * <p>Postgres: {@code app_user} rows are referenced by
     * {@code rental_request} foreign keys the agency's own booking ledger
     * depends on (V2 migration), so the row is anonymised -- PII columns
     * cleared -- rather than deleted, which would either violate the
     * constraint or destroy the tenant's transaction history. Firestore
     * {@code rental_requests}/{@code rental_reports} are left alone for the
     * same reason: they are the agency's business records, not the renter's
     * personal data, and the roadmap's erasure scope names Auth, the profile
     * and verification documents, Storage, and biometric templates -- not the
     * booking ledger.
     *
     * <p>Firestore {@code users}/{@code verification_requests} (with its
     * {@code evidence} subcollection), the Storage objects under
     * {@code verification_evidence/{uid}/}, and the Firebase Auth account
     * itself are deleted outright: this is exactly the personal and biometric
     * data the erasure right covers.
     */
    @Transactional
    public DeleteAccountResponse deleteData(String uid) {
        appUserRepository.findByFirebaseUid(uid).ifPresent(user -> {
            user.setEmail(null);
            user.setFullName(null);
            user.setPhone(null);
            appUserRepository.save(user);
        });

        firestoreEraser.deleteCollection(VERIFICATION_REQUESTS_COLLECTION + "/" + uid + "/evidence");
        firestoreEraser.deleteDocument(VERIFICATION_REQUESTS_COLLECTION, uid);
        firestoreEraser.deleteDocument(USERS_COLLECTION, uid);
        firestoreEraser.deleteStoragePrefix(VERIFICATION_STORAGE_PREFIX + uid + "/");
        firestoreEraser.deleteAuthUser(uid);

        return new DeleteAccountResponse(true, Instant.now());
    }

    private static Map<String, Object> accountAsMap(AppUser user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("companyId", user.getCompanyId());
        map.put("email", user.getEmail());
        map.put("fullName", user.getFullName());
        map.put("phone", user.getPhone());
        map.put("role", user.getRole().getStorageValue());
        map.put("status", user.getStatus().getStorageValue());
        map.put("verificationStatus", user.getVerificationStatus().getStorageValue());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }

    private static Map<String, Object> bookingAsMap(RentalRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", request.getId());
        map.put("companyId", request.getCompanyId());
        map.put("carId", request.getCarId());
        map.put("startAt", request.getStartAt());
        map.put("endAt", request.getEndAt());
        map.put("status", request.getStatus().getStorageValue());
        map.put("totalPrice", request.getPricing().getTotalPrice());
        map.put("currency", request.getPricing().getCurrency());
        map.put("createdAt", request.getCreatedAt());
        return map;
    }
}
