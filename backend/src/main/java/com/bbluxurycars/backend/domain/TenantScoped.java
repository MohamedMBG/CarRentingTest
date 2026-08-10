package com.bbluxurycars.backend.domain;

/**
 * Marks an entity that belongs to exactly one tenant.
 *
 * <p>Tenant isolation is today enforced in a single place -- {@code
 * firestore.rules}. As data moves server-side that guarantee has to be rebuilt
 * here, and cross-tenant leakage is the failure mode that ends a B2B SaaS
 * (docs/SAAS_ROADMAP.md). This interface is the hook the isolation machinery
 * hangs off: {@code TenantScopedRepository} refuses to expose an unfiltered
 * read for anything implementing it.
 *
 * <p>An interface alone cannot prevent a hand-written query from omitting the
 * filter. Postgres row-level security is the structural fix and is planned for
 * the slice that introduces the first tenant-owned table written by many
 * different code paths.
 */
public interface TenantScoped {

    /**
     * The owning tenant, or {@code null} when the row is not yet linked to one
     * (a user exists in Firebase Auth from signup, before any company is
     * attached).
     */
    String getCompanyId();
}
