package com.bbluxurycars.backend.web;

import com.bbluxurycars.backend.domain.Car;
import com.bbluxurycars.backend.domain.PricingBreakdown;
import com.bbluxurycars.backend.repository.CarRepository;
import com.bbluxurycars.backend.security.FirebaseAuthFilter;
import com.bbluxurycars.backend.tenant.TenantContext;
import com.bbluxurycars.backend.tenant.TenantContextService;
import com.bbluxurycars.backend.web.dto.CarResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The caller's own fleet.
 *
 * <p>There is no {@code companyId} parameter anywhere on this controller: the
 * tenant comes from the verified uid, so there is no way to ask for another
 * agency's vehicles.
 */
@RestController
public class CarController {

    private final TenantContextService tenantContextService;
    private final CarRepository carRepository;

    public CarController(TenantContextService tenantContextService, CarRepository carRepository) {
        this.tenantContextService = tenantContextService;
        this.carRepository = carRepository;
    }

    /**
     * @param includeUnavailable admins managing a fleet need to see vehicles
     *        they have withdrawn or put into maintenance; renters browsing do
     *        not. Defaults to the renter's view, so an endpoint used carelessly
     *        shows less rather than more.
     */
    @GetMapping("/v1/cars")
    public List<CarResponse> cars(HttpServletRequest request,
                                  @RequestParam(defaultValue = "false") boolean includeUnavailable) {
        String uid = (String) request.getAttribute(FirebaseAuthFilter.UID_ATTRIBUTE);
        TenantContext context = tenantContextService.resolve(uid);
        if (!context.hasTenantScope()) {
            // Unprovisioned callers are normal during the Firestore backfill:
            // an empty fleet, not an error (docs/SAAS_ROADMAP.md 5.3).
            return List.of();
        }

        List<Car> cars = includeUnavailable && context.isActiveAdmin()
                ? carRepository.findAllByCompanyId(context.companyId())
                : carRepository.findAllByCompanyIdAndAvailableTrueAndMaintenanceFalse(context.companyId());

        return cars.stream()
                .map(car -> CarResponse.from(car, PricingBreakdown.DEFAULT_CURRENCY))
                .toList();
    }
}
