package com.bbluxurycars.backend.repository;

import com.bbluxurycars.backend.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Users are addressed two different ways, and the split is the point.
 *
 * <p>{@link #findByFirebaseUid(String)} is an identity lookup: the caller has
 * proved ownership of a uid by presenting a verified Firebase token, and no
 * tenant is known yet -- resolving one is the whole purpose of the call. It is
 * therefore not tenant-scoped, and must never be used to fetch a user the
 * caller merely named.
 *
 * <p>Everything else goes through {@link TenantScopedRepository}, which forces
 * a tenant on every read.
 */
public interface AppUserRepository extends TenantScopedRepository<AppUser, String> {

    /**
     * Looks up the authenticated caller by their own uid.
     *
     * <p>Named for the caller's identity rather than {@code findById} so that
     * an untenanted read is impossible to write by accident and obvious in
     * review.
     */
    @Query("select u from AppUser u where u.id = :uid")
    Optional<AppUser> findByFirebaseUid(@Param("uid") String uid);

    AppUser save(AppUser user);
}
