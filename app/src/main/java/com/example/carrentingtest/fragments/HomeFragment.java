package com.example.carrentingtest.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.carrentingtest.R;
import com.example.carrentingtest.RentalFormActivity;
import com.example.carrentingtest.adapters.CarAdapter;
import com.example.carrentingtest.models.Car;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private ListView carsListView;
    private CarAdapter carAdapter;
    private List<Car> carList;
    private List<Car> filteredCarList; // For search/filter functionality
    private FirebaseFirestore db;
    private String companyId;
    private SearchView searchView;
    private LinearLayout filterContainer;
    private String currentFilterType = "All"; // Default filter
    private String currentSearchQuery = ""; // Default search query

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_home, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            db.collection("users").document(auth.getCurrentUser().getUid()).get()
                    .addOnSuccessListener(doc -> {
                        companyId = doc.getString("companyId");
                        fetchCars();
                    });
        }

        // Initialize views
        carsListView = view.findViewById(R.id.carsListView);
        searchView = view.findViewById(R.id.searchView);
        filterContainer = view.findViewById(R.id.filterContainer);

        // Initialize car lists
        carList = new ArrayList<>();
        filteredCarList = new ArrayList<>();
        carAdapter = new CarAdapter(requireContext(), filteredCarList); // Use filtered list for adapter
        carsListView.setAdapter(carAdapter);

        // Set item click listener for ListView
        carsListView.setOnItemClickListener((parent, view1, position, id) -> {
            Car selectedCar = filteredCarList.get(position); // Get car from filtered list
            if (selectedCar.isAvailable()) {
                Intent intent = new Intent(getActivity(), RentalFormActivity.class);
                intent.putExtra("selectedCar", selectedCar); // Pass Car object
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "This car is currently unavailable.", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup search functionality
        setupSearchView();

        // Setup filter functionality
        setupFilters(view);
    }

    private void fetchCars() {
        if (companyId == null) return;
        db.collection("cars")
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        carList.clear(); // Clear previous data
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Car car = document.toObject(Car.class);
                            car.setDocumentId(document.getId()); // Set the document ID
                            carList.add(car);
                        }
                        Log.d(TAG, "Successfully fetched " + carList.size() + " cars.");
                        // Apply initial filters (default: All, no search)
                        applyFilters();
                    } else {
                        Log.e(TAG, "Error getting documents: ", task.getException());
                        Toast.makeText(getContext(), "Error fetching cars: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                applyFilters();
                return false; // Let the SearchView handle the default action
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                applyFilters();
                return true; // We handled the text change
            }
        });
    }

    private void setupFilters(View view) {
        // Get references to filter buttons
        MaterialButton btnAll = view.findViewById(R.id.btnAll);
        MaterialButton btnSUV = view.findViewById(R.id.btnSUV);
        MaterialButton btnCompact = view.findViewById(R.id.btnCompact);
        MaterialButton btnLuxury = view.findViewById(R.id.btnLuxury);

        View.OnClickListener filterClickListener = v -> {
            String type = "All"; // Default
            int id = v.getId();
            if (id == R.id.btnSUV) {
                type = "SUV";
            } else if (id == R.id.btnCompact) {
                type = "Compact";
            } else if (id == R.id.btnLuxury) {
                type = "Luxury";
            }
            // else if (id == R.id.btnAll) { type = "All"; }

            currentFilterType = type;
            applyFilters();
            updateButtonStyles();
        };

        btnAll.setOnClickListener(filterClickListener);
        btnSUV.setOnClickListener(filterClickListener);
        btnCompact.setOnClickListener(filterClickListener);
        btnLuxury.setOnClickListener(filterClickListener);

        // Set initial style
        updateButtonStyles();
    }

    private void applyFilters() {
        filteredCarList.clear();
        String lowerCaseQuery = currentSearchQuery.toLowerCase().trim();

        for (Car car : carList) {
            boolean typeMatch = currentFilterType.equalsIgnoreCase("All") ||
                    (car.getType() != null && car.getType().equalsIgnoreCase(currentFilterType));

            boolean searchMatch = lowerCaseQuery.isEmpty() ||
                    (car.getModel() != null && car.getModel().toLowerCase().contains(lowerCaseQuery));
            // Add other search fields if needed
            // || (car.getType() != null && car.getType().toLowerCase().contains(lowerCaseQuery))

            if (typeMatch && searchMatch) {
                filteredCarList.add(car);
            }
        }
        carAdapter.notifyDataSetChanged(); // Update the ListView
        Log.d(TAG, "Applied filters. Type: " + currentFilterType + ", Query: '" + currentSearchQuery + "'. Filtered list size: " + filteredCarList.size());
    }

    private void updateButtonStyles() {
        int selectedColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        ColorStateList selectedTextColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorOnPrimary));
        int defaultColor = ContextCompat.getColor(requireContext(), R.color.colorSurface); // Or another default background
        ColorStateList defaultTextColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        ColorStateList defaultStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary));

        for (int i = 0; i < filterContainer.getChildCount(); i++) {
            View child = filterContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                String buttonType = button.getText().toString();

                if (buttonType.equalsIgnoreCase(currentFilterType)) {
                    // Selected style
                    button.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
                    button.setTextColor(selectedTextColor);
                    button.setStrokeWidth(0); // Remove stroke for selected filled button
                }
            }
        }
    }
}