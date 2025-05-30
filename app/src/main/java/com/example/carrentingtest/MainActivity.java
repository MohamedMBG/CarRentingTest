package com.example.carrentingtest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import com.example.carrentingtest.adapters.CarAdapter;
import com.example.carrentingtest.models.Car;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ListView carsListView;
    private CarAdapter carAdapter;
    private List<Car> carList;
    private List<Car> filteredCarList; // For filtered results
    private FirebaseFirestore db;
    private SearchView searchView;
    private String currentFilter = "all"; // Default filter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            db = FirebaseFirestore.getInstance();
            carsListView = findViewById(R.id.carsListView);
            searchView = findViewById(R.id.searchView);

            if (carsListView == null) {
                throw new RuntimeException("ListView not found - check XML ID");
            }

            carList = new ArrayList<>();
            filteredCarList = new ArrayList<>();
            carAdapter = new CarAdapter(this, filteredCarList);
            carsListView.setAdapter(carAdapter);

            setupSearchView();
            setupCategoryFilters();
            loadAvailableCars();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }

        carsListView.setOnItemClickListener((parent, view, position, id) -> {
            Car selectedCar = filteredCarList.get(position);
            Log.d("CLICK_TEST", "Clicked on: " + selectedCar.getModel());

            if (selectedCar.isAvailable()) {
                openRentalForm(selectedCar);
            } else {
                Toast.makeText(this, "This car is not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCars(newText, currentFilter);
                return true;
            }
        });
    }

    private void setupCategoryFilters() {
        findViewById(R.id.btnAll).setOnClickListener(v -> {
            currentFilter = "all";
            filterCars(searchView.getQuery().toString(), currentFilter);
        });

        findViewById(R.id.btnSUV).setOnClickListener(v -> {
            currentFilter = "SUV";
            filterCars(searchView.getQuery().toString(), currentFilter);
        });

        findViewById(R.id.btnCompact).setOnClickListener(v -> {
            currentFilter = "Compacte";
            filterCars(searchView.getQuery().toString(), currentFilter);
        });

        findViewById(R.id.btnLuxury).setOnClickListener(v -> {
            currentFilter = "Luxe";
            filterCars(searchView.getQuery().toString(), currentFilter);
        });
    }

    private void filterCars(String searchText, String categoryFilter) {
        filteredCarList.clear();

        for (Car car : carList) {
            boolean matchesSearch = car.getModel().toLowerCase().contains(searchText.toLowerCase());
            boolean matchesCategory = categoryFilter.equals("all") ||
                    car.getType().equalsIgnoreCase(categoryFilter);

            if (matchesSearch && matchesCategory) {
                filteredCarList.add(car);
            }
        }

        carAdapter.notifyDataSetChanged();

        if (filteredCarList.isEmpty()) {
            Toast.makeText(this, "No cars found matching your criteria", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAvailableCars() {
        db.collection("cars")
                .whereEqualTo("available", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        carList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Car car = document.toObject(Car.class);
                                car.setDocumentId(document.getId());
                                carList.add(car);
                            } catch (Exception e) {
                                Log.e("Firestore", "Error parsing car", e);
                            }
                        }
                        // After loading, apply any existing filters
                        filterCars(searchView.getQuery().toString(), currentFilter);
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Error loading cars: " + (task.getException() != null ?
                                        task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openRentalForm(Car car) {
        Intent intent = new Intent(this, RentalFormActivity.class);
        intent.putExtra("selectedCar", car);
        startActivity(intent);
    }
}