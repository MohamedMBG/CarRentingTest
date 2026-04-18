package com.example.carrentingtest.domain.usecase;

import androidx.annotation.NonNull;

import com.example.carrentingtest.data.repository.CarRepository;
import com.example.carrentingtest.data.session.TenantSessionProvider;
import com.example.carrentingtest.models.Car;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;

public class LoadTenantCarsUseCase {

    private final TenantSessionProvider tenantSessionProvider;
    private final CarRepository carRepository;

    public LoadTenantCarsUseCase() {
        this(new TenantSessionProvider(), new CarRepository());
    }

    LoadTenantCarsUseCase(@NonNull TenantSessionProvider tenantSessionProvider,
                          @NonNull CarRepository carRepository) {
        this.tenantSessionProvider = tenantSessionProvider;
        this.carRepository = carRepository;
    }

    public Task<List<Car>> execute(boolean availableOnly) {
        return tenantSessionProvider.requireTenantContext()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        return Tasks.forException(
                                task.getException() != null
                                        ? task.getException()
                                        : new IllegalStateException("Tenant context could not be resolved."));
                    }
                    String companyId = task.getResult().getCompanyId();
                    if (availableOnly) {
                        return carRepository.getAvailableCarsForCompany(companyId);
                    }
                    return carRepository.getCarsForCompany(companyId);
                });
    }
}
