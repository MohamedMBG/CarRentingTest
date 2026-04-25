package com.example.carrentingtest.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.carrentingtest.R;
import com.example.carrentingtest.RentalFormActivity;
import com.example.carrentingtest.adapters.CarGridAdapter;
import com.example.carrentingtest.domain.usecase.LoadTenantCarsUseCase;
import com.example.carrentingtest.models.Car;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private RecyclerView carsRecyclerView;
    private CarGridAdapter carAdapter;
    private List<Car> carList;
    private List<Car> filteredCarList; // For search/filter functionality
    private SearchView searchView;
    private LinearLayout filterContainer;
    private TextView tvHomeResultsCount;
    private TextView tvHomeSelectedFilter;
    private TextView tvHomeEmptyTitle;
    private TextView tvHomeEmptyBody;
    private View homeEmptyStateCard;
    private String currentFilterType = "All"; // Default filter
    private String currentSearchQuery = ""; // Default search query
    private LoadTenantCarsUseCase loadTenantCarsUseCase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadTenantCarsUseCase = new LoadTenantCarsUseCase();

        // Initialize views
        carsRecyclerView = view.findViewById(R.id.carsRecyclerView);
        searchView = view.findViewById(R.id.searchView);
        filterContainer = view.findViewById(R.id.filterContainer);
        tvHomeResultsCount = view.findViewById(R.id.tvHomeResultsCount);
        tvHomeSelectedFilter = view.findViewById(R.id.tvHomeSelectedFilter);
        tvHomeEmptyTitle = view.findViewById(R.id.tvHomeEmptyTitle);
        tvHomeEmptyBody = view.findViewById(R.id.tvHomeEmptyBody);
        homeEmptyStateCard = view.findViewById(R.id.homeEmptyStateCard);

        // Initialize car lists
        carList = new ArrayList<>();
        filteredCarList = new ArrayList<>();
        carAdapter = new CarGridAdapter(filteredCarList, (selectedCar, sharedImageView) -> {
            if (selectedCar.isMaintenance()) {
                Toast.makeText(getContext(), R.string.request_car_maintenance, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(getActivity(), RentalFormActivity.class);
            intent.putExtra("selectedCar", selectedCar);

            String transitionName = androidx.core.view.ViewCompat.getTransitionName(sharedImageView);
            androidx.core.app.ActivityOptionsCompat options = androidx.core.app.ActivityOptionsCompat
                    .makeSceneTransitionAnimation(
                            getActivity(),
                            sharedImageView,
                            transitionName);

            startActivity(intent, options.toBundle());
        });
        carsRecyclerView.setAdapter(carAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false);
        carsRecyclerView.setLayoutManager(layoutManager);
        carsRecyclerView.setClipToPadding(false);
        carsRecyclerView.setHasFixedSize(true);

        // Setup search functionality
        setupSearchView();

        // Setup filter functionality
        setupFilters(view);

        // Setup AI Concierge FAB
        view.findViewById(R.id.fab_concierge).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), com.example.carrentingtest.ui.ai.ConciergeActivity.class));
        });

        fetchCars();
    }

    private void fetchCars() {
        loadTenantCarsUseCase.execute(false)
                .addOnSuccessListener(cars -> {
                    carList.clear();
                    carList.addAll(cars);
                    Log.d(TAG, "Successfully fetched " + carList.size() + " cars.");
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    carList.clear();
                    filteredCarList.clear();
                    applyFilters();
                    Log.e(TAG, "Error getting tenant-scoped cars", e);
                    if (!isAdded() || getContext() == null) return;
                    String message = e.getMessage();
                    if (message == null) {
                        message = "Failed to load cars. Please try again.";
                    }
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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
            // || (car.getType() != null &&
            // car.getType().toLowerCase().contains(lowerCaseQuery))

            if (typeMatch && searchMatch) {
                filteredCarList.add(car);
            }
        }
        carAdapter.notifyDataSetChanged(); // Update the RecyclerView grid
        updateHomeState();
        Log.d(TAG, "Applied filters. Type: " + currentFilterType + ", Query: '" + currentSearchQuery
                + "'. Filtered list size: " + filteredCarList.size());
    }

    private void updateHomeState() {
        if (!isAdded()) {
            return;
        }

        int resultCount = filteredCarList.size();
        tvHomeResultsCount.setText(getResources().getQuantityString(
                R.plurals.home_vehicle_count,
                resultCount,
                resultCount));

        if ("All".equalsIgnoreCase(currentFilterType)) {
            tvHomeSelectedFilter.setText(R.string.home_filter_all_value);
        } else {
            tvHomeSelectedFilter.setText(currentFilterType);
        }

        boolean showEmptyState = resultCount == 0;
        homeEmptyStateCard.setVisibility(showEmptyState ? View.VISIBLE : View.GONE);
        carsRecyclerView.setVisibility(showEmptyState ? View.GONE : View.VISIBLE);

        if (!showEmptyState) {
            return;
        }

        if (carList.isEmpty()) {
            tvHomeEmptyTitle.setText(R.string.home_empty_inventory_title);
            tvHomeEmptyBody.setText(R.string.home_empty_inventory_body);
            return;
        }

        tvHomeEmptyTitle.setText(R.string.home_empty_filtered_title);
        tvHomeEmptyBody.setText(R.string.home_empty_filtered_body);
    }

    private void updateButtonStyles() {
        int selectedColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary);
        ColorStateList selectedTextColor = ColorStateList
                .valueOf(ContextCompat.getColor(requireContext(), R.color.colorOnPrimary));
        int defaultColor = ContextCompat.getColor(requireContext(), R.color.homeFilterDefaultBackground);
        ColorStateList defaultTextColor = ColorStateList
                .valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        ColorStateList defaultStrokeColor = ColorStateList
                .valueOf(ContextCompat.getColor(requireContext(), R.color.homeFilterStroke));
        ColorStateList rippleColor = ColorStateList
                .valueOf(ContextCompat.getColor(requireContext(), R.color.homeFilterRipple));
        int strokeWidth = getResources().getDimensionPixelSize(R.dimen.home_filter_chip_stroke_width);

        for (int i = 0; i < filterContainer.getChildCount(); i++) {
            View child = filterContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                String buttonType = button.getText().toString();
                boolean isSelected = buttonType.equalsIgnoreCase(currentFilterType);

                button.setRippleColor(rippleColor);

                if (isSelected) {
                    button.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
                    button.setTextColor(selectedTextColor);
                    button.setStrokeWidth(0);
                } else {
                    button.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
                    button.setTextColor(defaultTextColor);
                    button.setStrokeColor(defaultStrokeColor);
                    button.setStrokeWidth(strokeWidth);
                }
            }
        }
    }
}
