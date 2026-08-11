package com.bbluxurycars.backend.firestore;

import java.util.List;
import java.util.Optional;

/**
 * The narrow slice of Firestore the mirror needs: read a document, or read the
 * documents of one tenant.
 *
 * <p>An interface rather than direct SDK calls for two reasons. It keeps the
 * mirror testable without a Firestore emulator or network, and it states the
 * access the backend actually has -- reads only. The backend must not write to
 * Firestore during the migration: the app owns those documents, and two writers
 * with no shared transaction would produce conflicts nobody can reconstruct
 * afterwards (docs/SAAS_ROADMAP.md 5.3).
 */
public interface FirestoreGateway {

    /**
     * Whether Firestore can be reached at all. False when the service is
     * running without Firebase credentials, which is the normal local and test
     * configuration -- callers degrade instead of failing.
     */
    boolean isAvailable();

    Optional<FirestoreDocument> findDocument(String collection, String id);

    /**
     * Every document in {@code collection} whose {@code field} equals
     * {@code value}. Used only with {@code companyId} today, which is the
     * tenant predicate on all three mirrored collections.
     */
    List<FirestoreDocument> findWhereEquals(String collection, String field, String value);
}
