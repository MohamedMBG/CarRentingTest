package com.example.carrentingtest.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.R;
import com.example.carrentingtest.utils.FullscreenUiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AdminAgencyLocationActivity extends AppCompatActivity {

    private TextInputLayout tilAddress;
    private TextInputLayout tilLatitude;
    private TextInputLayout tilLongitude;
    private TextInputEditText etAddress;
    private TextInputEditText etLatitude;
    private TextInputEditText etLongitude;
    private MaterialButton btnSaveLocation;
    private CircularProgressIndicator progressIndicator;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String companyId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_agency_location);
        FullscreenUiHelper.apply(this, R.id.admin_agency_location_root);

        tilAddress = findViewById(R.id.tilAgencyAddress);
        tilLatitude = findViewById(R.id.tilAgencyLatitude);
        tilLongitude = findViewById(R.id.tilAgencyLongitude);
        etAddress = findViewById(R.id.etAgencyAddress);
        etLatitude = findViewById(R.id.etAgencyLatitude);
        etLongitude = findViewById(R.id.etAgencyLongitude);
        btnSaveLocation = findViewById(R.id.btnSaveLocation);
        progressIndicator = findViewById(R.id.progressIndicator);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnSaveLocation.setOnClickListener(v -> attemptSave());

        loadCompanyData();
    }

    private void loadCompanyData() {
        toggleLoading(true);
        AdminAccessManager.guardOperationalAccess(this, db, access -> {
            companyId = access.getCompanyId();
            fetchCompany();
        });
    }

    private void fetchCompany() {
        db.collection("companies")
                .document(companyId)
                .get()
                .addOnSuccessListener(this::bindCompany)
                .addOnFailureListener(e -> {
                    toggleLoading(false);
                    Toast.makeText(this, R.string.location_load_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void bindCompany(DocumentSnapshot snapshot) {
        if (snapshot != null && snapshot.exists()) {
            String address = snapshot.getString("address");
            GeoPoint location = snapshot.getGeoPoint("location");
            Double latitude = location != null ? location.getLatitude() : snapshot.getDouble("locationLat");
            Double longitude = location != null ? location.getLongitude() : snapshot.getDouble("locationLng");

            if (!TextUtils.isEmpty(address)) {
                etAddress.setText(address);
            }

            if (latitude != null) {
                etLatitude.setText(String.format(Locale.US, "%f", latitude));
            }

            if (longitude != null) {
                etLongitude.setText(String.format(Locale.US, "%f", longitude));
            }
        }

        toggleLoading(false);
    }

    private void attemptSave() {
        clearErrors();

        String address = Objects.requireNonNull(etAddress.getText()).toString().trim();
        String latText = Objects.requireNonNull(etLatitude.getText()).toString().trim();
        String lngText = Objects.requireNonNull(etLongitude.getText()).toString().trim();

        boolean isValid = true;
        Double latitude = null;
        Double longitude = null;

        if (TextUtils.isEmpty(address)) {
            tilAddress.setError(getString(R.string.location_address_required));
            isValid = false;
        }

        boolean hasLat = !TextUtils.isEmpty(latText);
        boolean hasLng = !TextUtils.isEmpty(lngText);

        if (hasLat || hasLng) {
            if (!hasLat || !hasLng) {
                tilLatitude.setError(getString(R.string.location_coordinates_required));
                tilLongitude.setError(getString(R.string.location_coordinates_required));
                isValid = false;
            } else {
                try {
                    latitude = Double.parseDouble(latText);
                    if (latitude < -90d || latitude > 90d) {
                        tilLatitude.setError(getString(R.string.location_invalid_latitude));
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    tilLatitude.setError(getString(R.string.location_invalid_latitude));
                    isValid = false;
                }

                try {
                    longitude = Double.parseDouble(lngText);
                    if (longitude < -180d || longitude > 180d) {
                        tilLongitude.setError(getString(R.string.location_invalid_longitude));
                        isValid = false;
                    }
                } catch (NumberFormatException e) {
                    tilLongitude.setError(getString(R.string.location_invalid_longitude));
                    isValid = false;
                }
            }
        }

        if (!isValid || TextUtils.isEmpty(companyId)) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("address", address);
        if (latitude != null && longitude != null) {
            updates.put("location", new GeoPoint(latitude, longitude));
            updates.put("locationLat", latitude);
            updates.put("locationLng", longitude);
        } else {
            updates.put("location", null);
            updates.put("locationLat", null);
            updates.put("locationLng", null);
        }

        toggleLoading(true);
        db.collection("companies")
                .document(companyId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    toggleLoading(false);
                    Toast.makeText(this, R.string.location_save_success, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    toggleLoading(false);
                    Toast.makeText(this, R.string.location_save_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void clearErrors() {
        tilAddress.setError(null);
        tilLatitude.setError(null);
        tilLongitude.setError(null);
    }

    private void toggleLoading(boolean isLoading) {
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveLocation.setEnabled(!isLoading);
        etAddress.setEnabled(!isLoading);
        etLatitude.setEnabled(!isLoading);
        etLongitude.setEnabled(!isLoading);
    }
}
