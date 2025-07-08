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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

// Declares the HomeFragment class that extends Android's Fragment class
public class HomeFragment extends Fragment {

    // UI components
    private ListView carsListView;  // Displays the list of cars in a scrollable view
    private CarAdapter carAdapter;  // Custom adapter to display Car objects in the ListView

    // Data storage
    private List<Car> allCars = new ArrayList<>();  // Stores all car objects from database
    private FirebaseFirestore db = FirebaseFirestore.getInstance();  // Firestore database instance

    // Filter states
    private String currentFilter = "All";  // Tracks currently selected car type filter
    private String currentSearch = "";  // Tracks current search query text

    // Called when fragment creates its view
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflates (creates) the fragment's layout from XML
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Gets reference to ListView from layout
        carsListView = view.findViewById(R.id.carsListView);
        // Creates adapter with empty list (will be filled later)
        carAdapter = new CarAdapter(requireContext(), new ArrayList<>());
        // Connects adapter to ListView
        carsListView.setAdapter(carAdapter);

        // Sets click listener for list items
        carsListView.setOnItemClickListener((parent, view1, position, id) -> {
            // Gets clicked car from adapter
            Car car = carAdapter.getItem(position);
            if (car.isAvailable()) {
                // If available, start rental activity with car data
                startActivity(new Intent(getActivity(), RentalFormActivity.class)
                        .putExtra("selectedCar", car));
            } else {
                // Show message if car unavailable
                Toast.makeText(getContext(), "Car unavailable", Toast.LENGTH_SHORT).show();
            }
        });

        // Gets reference to search box
        SearchView searchView = view.findViewById(R.id.searchView);
        // Sets search text listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String s) { return false; }  // Not used
            @Override public boolean onQueryTextChange(String s) {
                currentSearch = s;  // Updates search text
                filterCars();  // Filters list
                return true;
            }
        });

        // Gets container with filter buttons
        LinearLayout filters = view.findViewById(R.id.filterContainer);
        // Sets click listeners for all filter buttons
        for (int i = 0; i < filters.getChildCount(); i++) {
            filters.getChildAt(i).setOnClickListener(v -> {
                // Gets button text as filter type
                currentFilter = ((MaterialButton)v).getText().toString();
                filterCars();  // Applies filter
            });
        }

        loadCars();  // Loads car data
        return view;  // Returns the created view
    }

    // Loads cars from Firestore database
    private void loadCars() {
        // Gets all documents from "cars" collection
        db.collection("cars").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                allCars.clear();  // Clears old data
                // Processes each document
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    // Converts document to Car object
                    Car car = doc.toObject(Car.class);
                    // Saves document ID in car object
                    car.setDocumentId(doc.getId());
                    // Adds to master list
                    allCars.add(car);
                }
                filterCars();  // Shows filtered list
            } else {
                // Shows error if loading fails
                Toast.makeText(getContext(), "Error loading cars", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Filters cars based on current selections
    private void filterCars() {
        List<Car> filtered = new ArrayList<>();  // Temporary list for filtered cars
        String query = currentSearch.toLowerCase();  // Case-insensitive search

        // Checks each car against filters
        for (Car car : allCars) {
            // True if "All" selected or type matches
            boolean typeMatches = currentFilter.equals("All") ||
                    car.getType().equalsIgnoreCase(currentFilter);
            // True if no search or model contains search text
            boolean searchMatches = query.isEmpty() ||
                    car.getModel().toLowerCase().contains(query);

            // Adds car if both filters match
            if (typeMatches && searchMatches) {
                filtered.add(car);
            }
        }

        // Updates adapter with filtered list
        carAdapter.clear();
        carAdapter.addAll(filtered);
        carAdapter.notifyDataSetChanged();  // Refreshes ListView
    }
}