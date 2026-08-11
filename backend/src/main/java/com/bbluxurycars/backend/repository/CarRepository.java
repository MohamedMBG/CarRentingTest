package com.bbluxurycars.backend.repository;

import com.bbluxurycars.backend.domain.Car;

import java.util.List;

/**
 * Fleet reads, all tenant-scoped by construction -- see
 * {@link TenantScopedRepository} for why this hierarchy avoids
 * {@code JpaRepository}.
 */
public interface CarRepository extends TenantScopedRepository<Car, String> {

    /**
     * The bookable fleet: what a renter is allowed to see as offerable.
     * Vehicles switched off by the agency or in maintenance are excluded here
     * rather than filtered by each caller, so one forgetful endpoint cannot
     * offer a car the agency has withdrawn.
     */
    List<Car> findAllByCompanyIdAndAvailableTrueAndMaintenanceFalse(String companyId);

    Car save(Car car);
}
