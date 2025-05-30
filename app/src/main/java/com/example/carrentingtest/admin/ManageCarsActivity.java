package com.example.carrentingtest.admin;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.carrentingtest.R;
import com.example.carrentingtest.adapters.CarAdapter;
import com.example.carrentingtest.models.Car;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageCarsActivity extends AppCompatActivity {

    private ListView carsListView;
    private List<Car> carList;
    private CarAdapter carAdapter;
    private FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_cars);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Setup ListView
        carsListView = findViewById(R.id.carsListView);
        carList = new ArrayList<>();
        carAdapter = new CarAdapter(this, carList);
        carsListView.setAdapter(carAdapter);

        // Load cars from Firestore
        loadCars();

        // Setup Add Car button
        Button btnAddCar = findViewById(R.id.btnAddCar);
        btnAddCar.setOnClickListener(v -> showAddCarDialog());

        // Setup item click listener for edit/delete
        carsListView.setOnItemClickListener((parent, view, position, id) -> {
            showCarOptionsDialog(carList.get(position));
        });
    }

    private void loadCars() {
        db.collection("cars")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        carList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Car car = document.toObject(Car.class);
                            car.setDocumentId(document.getId());
                            carList.add(car);
                        }
                        carAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to load cars", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddCarDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_car_form, null);

        EditText etModel = dialogView.findViewById(R.id.etModel);
        EditText etType = dialogView.findViewById(R.id.etType);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        EditText etImageUrl = dialogView.findViewById(R.id.etImageUrl);

        builder.setView(dialogView)
                .setTitle("Add New Car")
                .setPositiveButton("Save", (dialog, which) -> {
                    Car car = new Car();
                    car.setModel(etModel.getText().toString());
                    car.setType(etType.getText().toString());
                    car.setPricePerDay(Double.parseDouble(etPrice.getText().toString()));
                    car.setImageUrl(etImageUrl.getText().toString());
                    car.setAvailable(true); // New cars are available by default

                    addCarToFirestore(car);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addCarToFirestore(Car car) {
        db.collection("cars")
                .add(car)
                .addOnSuccessListener(documentReference -> {
                    car.setDocumentId(documentReference.getId());
                    carList.add(car);
                    carAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Car added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add car", Toast.LENGTH_SHORT).show();
                });
    }

    private void showCarOptionsDialog(Car car) {
        String[] options = {"Edit", "Delete", "Cancel"};

        new AlertDialog.Builder(this)
                .setTitle("Car Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit
                            showEditCarDialog(car);
                            break;
                        case 1: // Delete
                            deleteCar(car);
                            break;
                    }
                })
                .show();
    }

    private void showEditCarDialog(Car car) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_car_form, null);

        EditText etModel = dialogView.findViewById(R.id.etModel);
        EditText etType = dialogView.findViewById(R.id.etType);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);
        EditText etImageUrl = dialogView.findViewById(R.id.etImageUrl);

        // Add Switch for availability
        SwitchCompat swAvailable = dialogView.findViewById(R.id.swAvailable);
        swAvailable.setVisibility(View.VISIBLE);
        swAvailable.setChecked(car.isAvailable());

        // Pre-fill form
        etModel.setText(car.getModel());
        etType.setText(car.getType());
        etPrice.setText(String.valueOf(car.getPricePerDay()));
        etImageUrl.setText(car.getImageUrl());

        builder.setView(dialogView)
                .setTitle("Edit Car")
                .setPositiveButton("Update", (dialog, which) -> {
                    car.setModel(etModel.getText().toString());
                    car.setType(etType.getText().toString());
                    car.setPricePerDay(Double.parseDouble(etPrice.getText().toString()));
                    car.setImageUrl(etImageUrl.getText().toString());
                    car.setAvailable(swAvailable.isChecked()); // Update availability

                    updateCarInFirestore(car);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateCarInFirestore(Car car) {
        db.collection("cars")
                .document(car.getDocumentId())
                .set(car)
                .addOnSuccessListener(aVoid -> {
                    carAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Car updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update car", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteCar(Car car) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this car?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("cars")
                            .document(car.getDocumentId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                carList.remove(car);
                                carAdapter.notifyDataSetChanged();
                                Toast.makeText(this, "Car deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete car", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}