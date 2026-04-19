package com.example.carrentingtest.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.carrentingtest.R;
import com.example.carrentingtest.data.repository.CompanyRepository;
import com.example.carrentingtest.data.session.TenantSessionProvider;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;

import java.util.Locale;

public class AgencyMapFragment extends Fragment {

    private View cardPickupLocation;
    private View emptyStateView;
    private TextView tvCompanyName;
    private TextView tvAddress;
    private TextView tvCoordinates;
    private MaterialButton btnOpenInMaps;
    private MaterialButton btnOpenInBrowser;

    private final TenantSessionProvider tenantSessionProvider = new TenantSessionProvider();
    private final CompanyRepository companyRepository = new CompanyRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agency_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cardPickupLocation = view.findViewById(R.id.cardPickupLocation);
        emptyStateView = view.findViewById(R.id.emptyStateView);
        tvCompanyName = view.findViewById(R.id.tvCompanyName);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvCoordinates = view.findViewById(R.id.tvCoordinates);
        btnOpenInMaps = view.findViewById(R.id.btnOpenInMaps);
        btnOpenInBrowser = view.findViewById(R.id.btnOpenInBrowser);

        btnOpenInMaps.setEnabled(false);
        btnOpenInBrowser.setEnabled(false);

        loadCompanyLocation();
    }

    private void loadCompanyLocation() {
        tenantSessionProvider.requireTenantContext()
                .addOnSuccessListener(context -> companyRepository.getById(context.getCompanyId())
                        .addOnSuccessListener(this::bindCompany)
                        .addOnFailureListener(e -> showEmptyState()))
                .addOnFailureListener(e -> showEmptyState());
    }

    private void bindCompany(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            showEmptyState();
            return;
        }

        String companyName = snapshot.getString("name");
        String address = snapshot.getString("address");
        GeoPoint location = snapshot.getGeoPoint("location");

        if (TextUtils.isEmpty(address) && location == null) {
            showEmptyState();
            return;
        }

        tvCompanyName.setText(!TextUtils.isEmpty(companyName)
                ? companyName
                : getString(R.string.map_tab_company_name_fallback));
        tvAddress.setText(!TextUtils.isEmpty(address)
                ? address
                : getString(R.string.pickup_address_unavailable));
        tvCoordinates.setText(formatCoordinates(location));

        btnOpenInMaps.setEnabled(true);
        btnOpenInBrowser.setEnabled(true);
        btnOpenInMaps.setOnClickListener(v -> openInMaps(location, address, companyName));
        btnOpenInBrowser.setOnClickListener(v -> openInBrowser(location, address));

        cardPickupLocation.setVisibility(View.VISIBLE);
        emptyStateView.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        if (cardPickupLocation != null) {
            cardPickupLocation.setVisibility(View.GONE);
        }
        if (emptyStateView != null) {
            emptyStateView.setVisibility(View.VISIBLE);
        }
    }

    private String formatCoordinates(@Nullable GeoPoint location) {
        if (location == null) {
            return getString(R.string.map_tab_no_coordinates);
        }
        return String.format(Locale.ENGLISH, "%.5f, %.5f",
                location.getLatitude(), location.getLongitude());
    }

    private void openInMaps(@Nullable GeoPoint location,
                            @Nullable String address,
                            @Nullable String companyName) {
        String label = !TextUtils.isEmpty(companyName)
                ? companyName
                : getString(R.string.pickup_location_title);
        String uri = buildGeoUri(location, address, label);
        if (uri == null) {
            Toast.makeText(requireContext(), R.string.location_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            openInBrowser(location, address);
        }
    }

    private void openInBrowser(@Nullable GeoPoint location, @Nullable String address) {
        String uri = buildOpenStreetMapUri(location, address);
        if (uri == null) {
            Toast.makeText(requireContext(), R.string.location_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.maps_app_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private String buildGeoUri(@Nullable GeoPoint location,
                               @Nullable String address,
                               @NonNull String label) {
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            return String.format(Locale.ENGLISH,
                    "geo:%f,%f?q=%f,%f(%s)",
                    latitude,
                    longitude,
                    latitude,
                    longitude,
                    Uri.encode(label));
        }

        if (!TextUtils.isEmpty(address)) {
            return "geo:0,0?q=" + Uri.encode(address);
        }
        return null;
    }

    @Nullable
    private String buildOpenStreetMapUri(@Nullable GeoPoint location, @Nullable String address) {
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            return String.format(Locale.ENGLISH,
                    "https://www.openstreetmap.org/?mlat=%f&mlon=%f#map=16/%f/%f",
                    latitude,
                    longitude,
                    latitude,
                    longitude);
        }

        if (!TextUtils.isEmpty(address)) {
            return "https://www.openstreetmap.org/search?query=" + Uri.encode(address);
        }
        return null;
    }
}
