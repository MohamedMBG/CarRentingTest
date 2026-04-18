package com.example.carrentingtest.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.carrentingtest.models.Car;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class CarRepository {

    private final FirebaseFirestore firestore;

    public CarRepository() {
        this(FirebaseFirestore.getInstance());
    }

    CarRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Task<List<Car>> getCarsForCompany(@Nullable String companyId) {
        if (companyId == null || companyId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Company id is required."));
        }
        return firestore.collection("cars")
                .whereEqualTo("companyId", companyId)
                .get()
                .continueWith(task -> mapCars(task.getResult()));
    }

    public Task<List<Car>> getAvailableCarsForCompany(@Nullable String companyId) {
        if (companyId == null || companyId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Company id is required."));
        }
        return firestore.collection("cars")
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("available", true)
                .whereEqualTo("maintenance", false)
                .get()
                .continueWith(task -> mapCars(task.getResult()));
    }

    public Task<Car> getById(@Nullable String carId) {
        if (carId == null || carId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Car id is required."));
        }
        return firestore.collection("cars")
                .document(carId)
                .get()
                .continueWith(task -> mapCar(task.getResult()));
    }

    @NonNull
    private List<Car> mapCars(@Nullable QuerySnapshot snapshot) {
        List<Car> cars = new ArrayList<>();
        if (snapshot == null) {
            return cars;
        }
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            Car car = document.toObject(Car.class);
            if (car == null) {
                continue;
            }
            car.setDocumentId(document.getId());
            cars.add(car);
        }
        return cars;
    }

    @Nullable
    private Car mapCar(@Nullable DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null || !documentSnapshot.exists()) {
            return null;
        }
        Car car = documentSnapshot.toObject(Car.class);
        if (car != null) {
            car.setDocumentId(documentSnapshot.getId());
        }
        return car;
    }
}
