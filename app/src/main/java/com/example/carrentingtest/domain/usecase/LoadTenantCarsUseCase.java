package com.example.carrentingtest.domain.usecase;

import androidx.annotation.NonNull;

import com.example.carrentingtest.data.repository.CarRepository;
import com.example.carrentingtest.data.session.TenantSessionProvider;
import com.example.carrentingtest.models.Car;
import com.google.android.gms.tasks.Task;

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
                    String companyId = task.getResult().getCompanyId();
                    if (availableOnly) {
                        return carRepository.getAvailableCarsForCompany(companyId);
                    }
                    return carRepository.getCarsForCompany(companyId);
                });
    }
}
