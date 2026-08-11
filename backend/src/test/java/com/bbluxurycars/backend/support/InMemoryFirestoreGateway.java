package com.bbluxurycars.backend.support;

import com.bbluxurycars.backend.firestore.FirestoreDocument;
import com.bbluxurycars.backend.firestore.FirestoreGateway;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A hand-written {@link FirestoreGateway} holding documents in a map.
 *
 * <p>Preferred over a mock because the mirror's behaviour depends on what the
 * documents contain -- missing fields, unknown status strings, a user naming a
 * company that does not exist. Stubbing call-by-call would encode the answers
 * the test expects; this stores documents and lets the mirror read them.
 */
public class InMemoryFirestoreGateway implements FirestoreGateway {

    private final Map<String, Map<String, FirestoreDocument>> collections = new LinkedHashMap<>();
    private boolean available = true;

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void clear() {
        collections.clear();
        available = true;
    }

    public InMemoryFirestoreGateway put(String collection, String id, Map<String, Object> fields) {
        collections.computeIfAbsent(collection, key -> new LinkedHashMap<>())
                .put(id, new FirestoreDocument(id, fields));
        return this;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public Optional<FirestoreDocument> findDocument(String collection, String id) {
        if (!available) {
            return Optional.empty();
        }
        return Optional.ofNullable(collections.getOrDefault(collection, Map.of()).get(id));
    }

    @Override
    public List<FirestoreDocument> findWhereEquals(String collection, String field, String value) {
        if (!available) {
            return List.of();
        }
        List<FirestoreDocument> matches = new ArrayList<>();
        collections.getOrDefault(collection, Map.of()).values().forEach(document -> {
            if (document.getString(field).filter(value::equals).isPresent()) {
                matches.add(document);
            }
        });
        return matches;
    }
}
