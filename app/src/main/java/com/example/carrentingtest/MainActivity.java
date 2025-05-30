package com.example.carrentingtest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize with error handling
        try {
            db = FirebaseFirestore.getInstance();
            carsListView = findViewById(R.id.carsListView);

            if (carsListView == null) {
                throw new RuntimeException("ListView not found - check XML ID");
            }

            carList = new ArrayList<>();
            carAdapter = new CarAdapter(this, carList);
            carsListView.setAdapter(carAdapter);

            loadAvailableCars();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }

        // Add item click listener
        carsListView.setOnItemClickListener((parent, view, position, id) -> {
            Car selectedCar = carList.get(position);
            Log.d("CLICK_TEST", "Clicked on: " + selectedCar.getModel()); // Add this line

            if (selectedCar.isAvailable()) {
                openRentalForm(selectedCar);
            } else {
                Toast.makeText(this, "This car is not available", Toast.LENGTH_SHORT).show();
            }
        });
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
                                car.setDocumentId(document.getId()); // Important for updates
                                carList.add(car);
                            } catch (Exception e) {
                                Log.e("Firestore", "Error parsing car", e);
                            }
                        }
                        carAdapter.notifyDataSetChanged();
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