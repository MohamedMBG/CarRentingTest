package com.example.carrentingtest.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import com.example.carrentingtest.R;
import com.example.carrentingtest.RentalFormActivity;
import com.example.carrentingtest.adapters.CarAdapter;
import com.example.carrentingtest.models.Car;
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
    private SearchView searchView;
    // Add filter buttons if needed

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

        // Initialize views
        carsListView = view.findViewById(R.id.carsListView);
        searchView = view.findViewById(R.id.searchView);
        // Initialize filter buttons if needed

        // Initialize car lists
        carList = new ArrayList<>();
        filteredCarList = new ArrayList<>();
        carAdapter = new CarAdapter(requireContext(), filteredCarList); // Use filtered list for adapter
        carsListView.setAdapter(carAdapter);

        // Fetch cars from Firestore
        fetchCars();

        // Set item click listener for ListView
        carsListView.setOnItemClickListener((parent, view1, position, id) -> {
            Car selectedCar = filteredCarList.get(position); // Get car from filtered list
            if (selectedCar.isAvailable()) {
                Intent intent = new Intent(getActivity(), RentalFormActivity.class);
                intent.putExtra("selectedCar", selectedCar); // Pass Car object (ensure Car implements Serializable or Parcelable)
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "This car is currently unavailable.", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup search functionality
        setupSearchView();

        // Setup filter functionality if needed
        // setupFilters(view);
    }

    private void fetchCars() {
        db.collection("cars")
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
                        // Initially, filtered list is the same as the full list
                        filterCars(""); // Apply empty filter to show all cars initially
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
                filterCars(query);
                return false; // Let the SearchView handle the default action (e.g., closing keyboard)
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCars(newText);
                return true; // We handled the text change
            }
        });
    }

    private void filterCars(String query) {
        filteredCarList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredCarList.addAll(carList); // Show all cars if query is empty
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (Car car : carList) {
                // Filter by model (case-insensitive)
                if (car.getModel() != null && car.getModel().toLowerCase().contains(lowerCaseQuery)) {
                    filteredCarList.add(car);
                }
                // Add other filter criteria if needed (e.g., type)
                // else if (car.getType() != null && car.getType().toLowerCase().contains(lowerCaseQuery)) {
                //     filteredCarList.add(car);
                // }
            }
        }
        carAdapter.notifyDataSetChanged(); // Update the ListView
        Log.d(TAG, "Filtered list size: " + filteredCarList.size());
    }

    // Add setupFilters method if implementing category filters
    /*
    private void setupFilters(View view) {
        // Get references to filter buttons (btnAll, btnSUV, etc.)
        // Set onClickListeners for each button
        // Inside listeners, call a method like filterCarsByType("SUV")
    }

    private void filterCarsByType(String type) {
        filteredCarList.clear();
        if (type == null || type.equalsIgnoreCase("All")) {
            filteredCarList.addAll(carList);
        } else {
            String lowerCaseType = type.toLowerCase();
            for (Car car : carList) {
                if (car.getType() != null && car.getType().toLowerCase().equals(lowerCaseType)) {
                    filteredCarList.add(car);
                }
            }
        }
        carAdapter.notifyDataSetChanged();
        // Update button styles to show active filter
    }
    */
}
