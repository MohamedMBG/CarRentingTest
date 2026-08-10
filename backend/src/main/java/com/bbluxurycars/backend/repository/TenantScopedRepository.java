package com.bbluxurycars.backend.repository;

import com.bbluxurycars.backend.domain.TenantScoped;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Base for repositories over tenant-owned entities.
 *
 * <p>It deliberately does <em>not</em> extend {@code JpaRepository} or
 * {@code CrudRepository}. Those supply {@code findAll()} and
 * {@code findById(id)}, which read across every tenant; inheriting them here
 * would put a one-call cross-tenant leak within easy reach of any caller and
 * make it invisible in review, since the offending method would never appear in
 * this file. Every read exposed here instead takes the tenant explicitly.
 *
 * <p>Extending {@code Repository} rather than nothing at all keeps Spring Data
 * query derivation working for subinterfaces.
 *
 * @param <T> the tenant-owned entity type
 * @param <I> the entity's identifier type
 */
@NoRepositoryBean
public interface TenantScopedRepository<T extends TenantScoped, I> extends Repository<T, I> {

    /**
     * Loads one row, but only if it belongs to {@code companyId}.
     *
     * <p>Returning empty for a row that exists under a different tenant is
     * intentional: it is indistinguishable from the row not existing, so an
     * attacker cannot probe for identifiers belonging to other agencies.
     */
    Optional<T> findByIdAndCompanyId(I id, String companyId);

    List<T> findAllByCompanyId(String companyId);

    long countByCompanyId(String companyId);
}
