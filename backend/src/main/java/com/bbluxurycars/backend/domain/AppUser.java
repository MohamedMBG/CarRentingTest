package com.bbluxurycars.backend.domain;

import com.bbluxurycars.backend.domain.converter.StoredEnumConverters;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * A person: an agency admin or a renter.
 *
 * <p>Named {@code AppUser} rather than {@code User} because {@code user} is a
 * reserved word in Postgres and an unquoted table of that name will not parse.
 *
 * <p>The identifier is the Firebase Auth uid. Firebase remains the identity
 * provider and the sole holder of credentials -- no password material is
 * mirrored here, and none should be.
 *
 * <p>{@code companyId} is a plain column rather than a {@code @ManyToOne} to
 * {@link Company}: nearly every read wants the tenant identifier for filtering
 * and not the company row itself, and an association would invite a lazy-load
 * per user on any list endpoint.
 */
@Entity
@Table(name = "app_user")
public class AppUser implements TenantScoped {

    @Id
    @Column(name = "id", nullable = false, length = 128)
    private String id;

    @Column(name = "company_id", length = 128)
    private String companyId;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "phone", length = 64)
    private String phone;

    @Column(name = "role", nullable = false, length = 32)
    private UserRole role = UserRole.UNKNOWN;

    @Convert(converter = StoredEnumConverters.UserLifecycleStatusConverter.class)
    @Column(name = "status", nullable = false, length = 64)
    private UserLifecycleStatus status = UserLifecycleStatus.ACTIVE;

    @Column(name = "verification_status", nullable = false, length = 32)
    private VerificationStatus verificationStatus = VerificationStatus.NOT_STARTED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
        // Required by JPA.
    }

    public AppUser(String id, UserRole role) {
        this.id = Objects.requireNonNull(id, "id");
        this.role = Objects.requireNonNull(role, "role");
        this.status = UserLifecycleStatus.from(null, role);
    }

    @PrePersist
    void onInsert() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    /** Whether this user is attached to a tenant at all. */
    public boolean hasTenantScope() {
        return companyId != null && !companyId.isBlank();
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = Objects.requireNonNull(role, "role");
    }

    public UserLifecycleStatus getStatus() {
        return status;
    }

    public void setStatus(UserLifecycleStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = Objects.requireNonNull(verificationStatus, "verificationStatus");
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof AppUser appUser && Objects.equals(id, appUser.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
