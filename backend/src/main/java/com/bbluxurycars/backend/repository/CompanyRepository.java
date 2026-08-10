package com.bbluxurycars.backend.repository;

import com.bbluxurycars.backend.domain.Company;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * The company table is the tenant table, so it is not itself tenant-scoped --
 * a company row's identifier <em>is</em> the tenant boundary, and scoping it
 * would be circular.
 *
 * <p>The safety rule for callers: only ever load the company whose id came from
 * the caller's own resolved tenant context, never one named in a request.
 */
public interface CompanyRepository extends CrudRepository<Company, String> {

    Optional<Company> findById(String id);
}
