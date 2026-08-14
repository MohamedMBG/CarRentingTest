package com.bbluxurycars.backend.support;

import com.bbluxurycars.backend.privacy.FirestoreEraser;

import java.util.ArrayList;
import java.util.List;

/**
 * Records what {@link com.bbluxurycars.backend.privacy.PrivacyService} asked to
 * have erased, rather than touching real Firebase. What matters to the tests
 * using this is which paths were named, not that a delete actually reached a
 * document -- {@link FirebaseFirestoreEraser} in production code is what does
 * the reaching.
 */
public class InMemoryFirestoreEraser implements FirestoreEraser {

    public final List<String> deletedDocuments = new ArrayList<>();
    public final List<String> deletedCollections = new ArrayList<>();
    public final List<String> deletedStoragePrefixes = new ArrayList<>();
    public final List<String> deletedAuthUsers = new ArrayList<>();

    public void clear() {
        deletedDocuments.clear();
        deletedCollections.clear();
        deletedStoragePrefixes.clear();
        deletedAuthUsers.clear();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void deleteDocument(String collection, String id) {
        deletedDocuments.add(collection + "/" + id);
    }

    @Override
    public void deleteCollection(String collectionPath) {
        deletedCollections.add(collectionPath);
    }

    @Override
    public void deleteStoragePrefix(String prefix) {
        deletedStoragePrefixes.add(prefix);
    }

    @Override
    public void deleteAuthUser(String uid) {
        deletedAuthUsers.add(uid);
    }
}
