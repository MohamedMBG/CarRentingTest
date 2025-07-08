package com.example.carrentingtest.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.carrentingtest.R;
import com.example.carrentingtest.adapters.CarAdapter;
import com.example.carrentingtest.models.Car;
import com.google.firebase.firestore.*;
import java.util.*;

public class ManageCarsActivity extends AppCompatActivity {
    // UI Components
    private ListView carsListView;
    // Data storage
    private List<Car> carList = new ArrayList<>();
    // Adapter for ListView
    private CarAdapter carAdapter;
    // Firestore database instance
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Initial setup when activity is created
     * - Sets up ListView and adapter
     * - Configures button click listeners
     * - Loads initial car data
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_cars);  // Set the layout for this activity

        // Initialize ListView and its adapter
        carsListView = findViewById(R.id.carsListView);
        carAdapter = new CarAdapter(this, carList);
        carsListView.setAdapter(carAdapter);

        // Set click listener for "Add Car" button
        findViewById(R.id.btnAddCar).setOnClickListener(v -> showCarDialog(null));

        // Set click listener for ListView items (for edit/delete)
        carsListView.setOnItemClickListener((p, v, pos, id) -> showOptionsDialog(carList.get(pos)));

        // Load initial car data from database
        loadCars();
    }

    /**
     * Loads car data from Firestore database
     * - Clears existing list
     * - Fetches all documents from 'cars' collection
     * - Updates adapter when data is loaded
     */
    private void loadCars() {
        db.collection("cars").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                carList.clear();  // Clear existing data
                // Process each document in the query result
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Car car = doc.toObject(Car.class);  // Convert document to Car object
                    car.setDocumentId(doc.getId());  // Store document ID for future reference
                    carList.add(car);  // Add to local list
                }
                carAdapter.notifyDataSetChanged();  // Refresh ListView
            } else {
                // Show error message if loading fails
                Toast.makeText(this, "Failed to load cars", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows a dialog for adding/editing a car
     * parameter car The car to edit (null for adding new car)
     */
    private void showCarDialog(Car car) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Inflate the custom dialog layout
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_car_form, null);

        // Get references to all form fields
        EditText etModel = view.findViewById(R.id.etModel),
                etType = view.findViewById(R.id.etType),
                etPrice = view.findViewById(R.id.etPrice),
                etImageUrl = view.findViewById(R.id.etImageUrl);
        SwitchCompat swAvailable = view.findViewById(R.id.swAvailable);

        boolean isEdit = car != null;  // Determine if we're editing or adding
        swAvailable.setVisibility(isEdit ? View.VISIBLE : View.GONE);  // Only show availability switch when editing

        // If editing, pre-fill the form with existing values
        if (isEdit) {
            etModel.setText(car.getModel());
            etType.setText(car.getType());
            etPrice.setText(String.valueOf(car.getPricePerDay()));
            etImageUrl.setText(car.getImageUrl());
            swAvailable.setChecked(car.isAvailable());
        }

        // Configure dialog buttons and behavior
        builder.setView(view)
                .setTitle(isEdit ? "Edit Car" : "Add New Car")  // Dynamic title
                .setPositiveButton(isEdit ? "Update" : "Save", (d, w) -> {
                    // Create or get the car object to save
                    Car c = isEdit ? car : new Car();
                    // Set all properties from form fields
                    c.setModel(etModel.getText().toString());
                    c.setType(etType.getText().toString());
                    c.setPricePerDay(Double.parseDouble(etPrice.getText().toString()));
                    c.setImageUrl(etImageUrl.getText().toString());
                    // Set availability (only for edits, new cars are available by default)
                    if (isEdit) c.setAvailable(swAvailable.isChecked());
                    else c.setAvailable(true);

                    // Call appropriate save method
                    if (isEdit) updateCar(c);
                    else addCar(c);
                })
                .setNegativeButton("Cancel", null)  // Cancel button does nothing
                .show();
    }

    /**
     * Adds a new car to Firestore database
     * parameter car The car object to add
     */
    private void addCar(Car car) {
        db.collection("cars").add(car).addOnSuccessListener(doc -> {
            // On success: update local data and show confirmation
            car.setDocumentId(doc.getId());  // Store the auto-generated document ID
            carList.add(car);  // Add to local list
            carAdapter.notifyDataSetChanged();  // Refresh ListView
            Toast.makeText(this, "Car added", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e ->
                // Show error message if operation fails
                Toast.makeText(this, "Failed to add car", Toast.LENGTH_SHORT).show());
    }

    /**
     * Updates an existing car in Firestore
     * parameter car The car object with updated values
     */
    private void updateCar(Car car) {
        db.collection("cars").document(car.getDocumentId()).set(car)
                .addOnSuccessListener(v -> {
                    // On success: refresh UI and show confirmation
                    carAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Car updated", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e ->
                        // Show error message if operation fails
                        Toast.makeText(this, "Failed to update car", Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows options dialog for a car (edit/delete)
     * parameter car The car to perform actions on
     */
    private void showOptionsDialog(Car car) {
        new AlertDialog.Builder(this)
                .setTitle("Car Options")
                .setItems(new String[]{"Edit", "Delete", "Cancel"}, (d, which) -> {
                    // Handle option selection
                    if (which == 0) showCarDialog(car);  // Edit
                    else if (which == 1) deleteCar(car);  // Delete
                    // Cancel (which == 2) does nothing
                }).show();
    }

    /**
     * Shows confirmation dialog and deletes a car if confirmed
     * parameter car The car to delete
     */
    private void deleteCar(Car car) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Delete this car?")
                .setPositiveButton("Delete", (d, w) ->
                        // On confirm: delete from Firestore
                        db.collection("cars")
                                .document(car.getDocumentId())
                                .delete()
                                .addOnSuccessListener(v -> {
                                    // On success: update local data and show confirmation
                                    carList.remove(car);
                                    carAdapter.notifyDataSetChanged();
                                    Toast.makeText(this, "Car deleted", Toast.LENGTH_SHORT).show();
                                }).addOnFailureListener(e ->
                                        // Show error message if operation fails
                                        Toast.makeText(this, "Failed to delete car", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)  // Cancel button does nothing
                .show();
    }
}