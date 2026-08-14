package com.bbluxurycars.backend.privacy;

/**
 * The privileged, write-capable counterpart to
 * {@link com.bbluxurycars.backend.firestore.FirestoreGateway}.
 *
 * <p>{@code FirestoreGateway} is deliberately read-only -- the app owns
 * Firestore writes during the migration (docs/SAAS_ROADMAP.md 5.3). GDPR
 * erasure is the one case that must break that rule: a renter cannot delete
 * their own Auth record or another collection's copy of their data, and
 * Firestore security rules block it outright (see {@code verification_requests}
 * in {@code firestore.rules}, {@code allow delete: if false}). Only the Admin
 * SDK, running here, can do it -- so this interface exists as a separate,
 * narrowly-scoped door rather than adding writes to the mirror's contract.
 *
 * <p>Every method degrades to a no-op (logged) rather than throwing when
 * Firebase is unavailable, matching {@code FirestoreGateway}'s failure mode --
 * see {@link #isAvailable()}.
 */
public interface FirestoreEraser {

    boolean isAvailable();

    void deleteDocument(String collection, String id);

    /** Deletes every document directly inside a collection (or subcollection) path. */
    void deleteCollection(String collectionPath);

    /** Deletes every Storage object whose name starts with {@code prefix}. */
    void deleteStoragePrefix(String prefix);

    /** Deletes the Firebase Auth account itself. */
    void deleteAuthUser(String uid);
}
