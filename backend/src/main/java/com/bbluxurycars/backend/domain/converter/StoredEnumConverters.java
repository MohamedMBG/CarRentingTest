package com.bbluxurycars.backend.domain.converter;

import com.bbluxurycars.backend.domain.CompanyLifecycleStatus;
import com.bbluxurycars.backend.domain.UserLifecycleStatus;
import com.bbluxurycars.backend.domain.UserRole;
import com.bbluxurycars.backend.domain.VerificationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converters binding the domain enums to the lowercase strings the schema
 * stores.
 *
 * <p>{@code @Enumerated(EnumType.STRING)} would persist the Java constant name
 * ({@code PENDING_REVIEW}) and break the CHECK constraints in V1, which spell
 * the Firestore values ({@code pending_review}). Converters keep one spelling
 * across Firestore, Postgres and Java.
 *
 * <p>Reads route through each enum's {@code from(...)} rather than a strict
 * lookup: rows can predate a constraint, and a mirror should degrade to the
 * safe default instead of making a row unreadable.
 */
public final class StoredEnumConverters {

    private StoredEnumConverters() {
    }

    @Converter(autoApply = true)
    public static class UserRoleConverter implements AttributeConverter<UserRole, String> {

        @Override
        public String convertToDatabaseColumn(UserRole attribute) {
            return attribute == null ? UserRole.UNKNOWN.getStorageValue() : attribute.getStorageValue();
        }

        @Override
        public UserRole convertToEntityAttribute(String dbData) {
            return UserRole.from(dbData);
        }
    }

    @Converter(autoApply = true)
    public static class CompanyLifecycleStatusConverter
            implements AttributeConverter<CompanyLifecycleStatus, String> {

        @Override
        public String convertToDatabaseColumn(CompanyLifecycleStatus attribute) {
            return attribute == null
                    ? CompanyLifecycleStatus.PENDING_REVIEW.getStorageValue()
                    : attribute.getStorageValue();
        }

        @Override
        public CompanyLifecycleStatus convertToEntityAttribute(String dbData) {
            return CompanyLifecycleStatus.from(dbData);
        }
    }

    @Converter(autoApply = true)
    public static class VerificationStatusConverter
            implements AttributeConverter<VerificationStatus, String> {

        @Override
        public String convertToDatabaseColumn(VerificationStatus attribute) {
            return attribute == null
                    ? VerificationStatus.NOT_STARTED.getStorageValue()
                    : attribute.getStorageValue();
        }

        @Override
        public VerificationStatus convertToEntityAttribute(String dbData) {
            return VerificationStatus.from(dbData);
        }
    }

    /**
     * Applied explicitly by {@code AppUser} rather than {@code autoApply},
     * because {@link UserLifecycleStatus#from} takes a second argument and the
     * asymmetry is worth stating at the field that uses it.
     *
     * <p>The role passed on read is immaterial: it only selects the fallback
     * for an unrecognised value, and the {@code app_user_status_check}
     * constraint in V1 means the column can hold nothing but the three known
     * strings. The role-dependent default matters when ingesting raw Firestore
     * documents, which is where {@code from(raw, role)} is called directly.
     */
    @Converter
    public static class UserLifecycleStatusConverter
            implements AttributeConverter<UserLifecycleStatus, String> {

        @Override
        public String convertToDatabaseColumn(UserLifecycleStatus attribute) {
            return attribute == null
                    ? UserLifecycleStatus.ACTIVE.getStorageValue()
                    : attribute.getStorageValue();
        }

        @Override
        public UserLifecycleStatus convertToEntityAttribute(String dbData) {
            return UserLifecycleStatus.from(dbData, UserRole.CLIENT);
        }
    }
}
