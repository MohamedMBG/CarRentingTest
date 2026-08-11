package com.bbluxurycars.backend.firestore;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * One Firestore document as the mirror sees it: an id and a bag of fields.
 *
 * <p>Deliberately untyped. Firestore documents are written by several app
 * versions and have no schema, so a field may be missing, null, or of a
 * different type than expected. Every accessor here answers with
 * {@link Optional} rather than throwing, because a malformed field should cost
 * one column of one mirrored row, not the whole sync.
 *
 * @param id   the document id
 * @param data the raw field map; never null
 */
public record FirestoreDocument(String id, Map<String, Object> data) {

    /** A Firestore GeoPoint reduced to two primitives. */
    public record GeoPointValue(double latitude, double longitude) {
    }

    /**
     * Null-valued fields are dropped on entry rather than carried. Firestore
     * stores an explicit null as a real value, every accessor here would have
     * to guard against it, and {@code Map.copyOf} refuses one outright.
     */
    public FirestoreDocument {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (data != null) {
            data.forEach((key, value) -> {
                if (value != null) {
                    copy.put(key, value);
                }
            });
        }
        data = Collections.unmodifiableMap(copy);
    }

    public Optional<String> getString(String key) {
        Object value = data.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return Optional.of(text.trim());
        }
        return Optional.empty();
    }

    public Optional<Boolean> getBoolean(String key) {
        Object value = data.get(key);
        return value instanceof Boolean flag ? Optional.of(flag) : Optional.empty();
    }

    public Optional<Integer> getInteger(String key) {
        Object value = data.get(key);
        return value instanceof Number number ? Optional.of(number.intValue()) : Optional.empty();
    }

    /**
     * A money field.
     *
     * <p>Firestore stores numbers as doubles, so the value arrives with binary
     * floating-point error already baked in. Converting through
     * {@link BigDecimal#valueOf(double)} -- which goes via the shortest decimal
     * representation -- rounds it back to the number a human typed, rather than
     * to {@code 449.99999999999994}. Scaling is left to the caller, which knows
     * the column it is filling.
     */
    public Optional<BigDecimal> getDecimal(String key) {
        Object value = data.get(key);
        if (value instanceof BigDecimal decimal) {
            return Optional.of(decimal);
        }
        if (value instanceof Number number) {
            return Optional.of(BigDecimal.valueOf(number.doubleValue()));
        }
        return Optional.empty();
    }

    /**
     * A coordinate pair. The concrete Firestore {@code GeoPoint} type is
     * unwrapped by the gateway, so nothing outside this package has to depend
     * on the Firestore SDK to read one.
     */
    public Optional<GeoPointValue> getGeoPoint(String key) {
        Object value = data.get(key);
        return value instanceof GeoPointValue point ? Optional.of(point) : Optional.empty();
    }
}
