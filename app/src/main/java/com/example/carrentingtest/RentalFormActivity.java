// Package declaration for the activity
package com.example.carrentingtest;

// Import all required Android and Firebase libraries
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.carrentingtest.adapters.CarImagePagerAdapter;
import com.example.carrentingtest.data.repository.CompanyRepository;
import com.example.carrentingtest.data.repository.UserRepository;
import com.example.carrentingtest.domain.usecase.SubmitRentalRequestUseCase;
import com.example.carrentingtest.models.Car;
import com.example.carrentingtest.utils.NavUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;

public class RentalFormActivity extends AppCompatActivity {
    // Tag for logging purposes
    private static final String TAG = "RentalFormActivity";

    // The car being rented
    private Car selectedCar;

    // UI components
    private EditText etAdditionalRequests;
    private TextView tvStartDate, tvEndDate;
    private ViewPager2 vpCarImages;
    private TextView tvImageIndicator;
    private View adminContactCard;
    private View companyLocationCard;
    private TextView tvAdminName;
    private TextView tvAdminEmail;
    private TextView tvAdminPhone;
    private TextView tvPickupCompanyName;
    private TextView tvPickupAddress;
    private TextView tvCarType;
    private TextView tvCarPrice;
    private TextView tvCarSeats;
    private TextView tvCarTransmission;
    private TextView tvCarAvailability;
    private TextView tvCarRentalCount;
    private MaterialButton btnCallAdmin;
    private MaterialButton btnOpenMaps;
    private CarImagePagerAdapter imagePagerAdapter;
    private ViewPager2.OnPageChangeCallback imagePageChangeCallback;
    private String companyId;
    private String adminPhoneNumber;
    private GeoPoint companyLocation;
    private String companyAddress;
    private String companyDisplayName;

    // Date formatter for displaying dates
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    // Shared services
    private FirebaseAuth mAuth;
    private UserRepository userRepository;
    private CompanyRepository companyRepository;
    private SubmitRentalRequestUseCase submitRentalRequestUseCase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for this activity
        setContentView(R.layout.activity_rental_form);

        // Initialize shared data/domain services
        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        companyRepository = new CompanyRepository();
        submitRentalRequestUseCase = new SubmitRentalRequestUseCase();

        // Get the selected car passed from previous activity
        selectedCar = (Car) getIntent().getSerializableExtra("selectedCar");
        // If no car was passed, show error and close activity
        if (selectedCar == null) {
            Toast.makeText(this, "Error: Car details not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Use the car's associated company for routing requests
        companyId = selectedCar.getCompanyId();
        if (companyId == null) {
            Toast.makeText(this, "Error: Car company information not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize all UI components
        initializeViews();

        // Set up car details in the UI
        setupCarDetails();

        // Set up click listeners for buttons and date pickers
        setupClickListeners();

        // Fetch admin contact details for the selected company
        loadAdminContactInfo();
        loadCompanyLocation();
    }

    // Method to initialize all view references
    private void initializeViews() {
        etAdditionalRequests = findViewById(R.id.etAdditionalRequests);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        vpCarImages = findViewById(R.id.vpCarImages);
        tvImageIndicator = findViewById(R.id.tvImageIndicator);
        adminContactCard = findViewById(R.id.cardAdminContact);
        companyLocationCard = findViewById(R.id.cardCompanyLocation);
        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminEmail = findViewById(R.id.tvAdminEmail);
        tvAdminPhone = findViewById(R.id.tvAdminPhone);
        tvPickupCompanyName = findViewById(R.id.tvPickupCompanyName);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvCarType = findViewById(R.id.tvCarType);
        tvCarPrice = findViewById(R.id.tvCarPrice);
        tvCarSeats = findViewById(R.id.tvCarSeats);
        tvCarTransmission = findViewById(R.id.tvCarTransmission);
        tvCarAvailability = findViewById(R.id.tvCarAvailability);
        tvCarRentalCount = findViewById(R.id.tvCarRentalCount);
        btnCallAdmin = findViewById(R.id.btnCallAdmin);
        btnOpenMaps = findViewById(R.id.btnOpenInMaps);

        if (btnOpenMaps != null) {
            btnOpenMaps.setEnabled(false);
            btnOpenMaps.setAlpha(0.6f);
        }

        // Set transition name for shared element animation
        if (selectedCar != null) {
            androidx.core.view.ViewCompat.setTransitionName(vpCarImages, "car_image_" + selectedCar.getDocumentId());
        }
    }

    // Method to set up car details in the UI
    private void setupCarDetails() {
        // Set the car model text
        ((TextView) findViewById(R.id.tvSelectedCar)).setText(selectedCar.getModel());
        tvCarType.setText(!TextUtils.isEmpty(selectedCar.getType())
                ? selectedCar.getType()
                : getString(R.string.stat_placeholder_dash));
        tvCarPrice.setText(getString(R.string.price_per_day_format, selectedCar.getPricePerDay()));
        tvCarSeats.setText(selectedCar.getSeats() > 0
                ? getResources().getQuantityString(R.plurals.car_seats_count, selectedCar.getSeats(), selectedCar.getSeats())
                : getString(R.string.seats_unknown));
        tvCarTransmission.setText(!TextUtils.isEmpty(selectedCar.getTransmissionType())
                ? selectedCar.getTransmissionType()
                : getString(R.string.transmission_unknown));
        tvCarAvailability.setText(selectedCar.isMaintenance()
                ? getString(R.string.maintenance_status)
                : getString(selectedCar.isAvailable() ? R.string.car_available : R.string.car_unavailable));
        tvCarRentalCount.setText(selectedCar.getRentalCount() > 0
                ? getResources().getQuantityString(R.plurals.times_rented_value,
                        selectedCar.getRentalCount(),
                        selectedCar.getRentalCount())
                : getString(R.string.times_rented_never));

        List<String> imageUrls = selectedCar.getImageUrls();
        imagePagerAdapter = new CarImagePagerAdapter(this, imageUrls);
        vpCarImages.setAdapter(imagePagerAdapter);

        int imageCount = imagePagerAdapter.getItemCount();
        if (imageCount > 1) {
            tvImageIndicator.setVisibility(View.VISIBLE);
            tvImageIndicator.setText(getString(R.string.image_indicator_format, 1, imageCount));
            imagePageChangeCallback = new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    tvImageIndicator.setText(getString(R.string.image_indicator_format, position + 1, imageCount));
                }
            };
            vpCarImages.registerOnPageChangeCallback(imagePageChangeCallback);
        } else {
            tvImageIndicator.setVisibility(View.GONE);
        }
    }

    // Method to set up all click listeners
    private void setupClickListeners() {
        // Date picker for start date
        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        // Date picker for end date
        tvEndDate.setOnClickListener(v -> showDatePicker(false));
        // Submit button handler
        findViewById(R.id.btnSubmitRequest).setOnClickListener(v -> validateAndFetchLicense());

        if (btnOpenMaps != null) {
            btnOpenMaps.setOnClickListener(v -> openLocationInMaps());
        }
    }

    private void loadAdminContactInfo() {
        if (adminContactCard == null || btnCallAdmin == null) {
            return;
        }

        adminContactCard.setVisibility(View.GONE);
        btnCallAdmin.setEnabled(false);
        btnCallAdmin.setAlpha(0.6f);

        if (companyId == null) {
            return;
        }

        userRepository.getPrimaryAdminForCompany(companyId)
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        return;
                    }
                    String adminName = doc.getString("displayName");
                    String adminEmail = doc.getString("email");
                    adminPhoneNumber = doc.getString("phone");

                    tvAdminName.setText(!TextUtils.isEmpty(adminName) ? adminName : getString(R.string.admin));
                    tvAdminEmail.setText(!TextUtils.isEmpty(adminEmail) ? adminEmail : "-");

                    if (!TextUtils.isEmpty(adminPhoneNumber)) {
                        tvAdminPhone.setText(adminPhoneNumber);
                        btnCallAdmin.setEnabled(true);
                        btnCallAdmin.setAlpha(1f);
                        btnCallAdmin.setOnClickListener(v -> openDialer(adminPhoneNumber));
                    } else {
                        tvAdminPhone.setText(getString(R.string.no_phone_available));
                        btnCallAdmin.setOnClickListener(
                                v -> Toast.makeText(this, R.string.no_phone_available, Toast.LENGTH_SHORT).show());
                    }

                    adminContactCard.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to load admin contact", e));
    }

    private void loadCompanyLocation() {
        if (companyId == null || companyLocationCard == null) {
            return;
        }

        companyLocationCard.setVisibility(View.GONE);

        companyRepository.getById(companyId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        return;
                    }

                    companyDisplayName = documentSnapshot.getString("name");
                    companyAddress = documentSnapshot.getString("address");
                    companyLocation = documentSnapshot.getGeoPoint("location");

                    if (tvPickupCompanyName != null) {
                        tvPickupCompanyName.setText(!TextUtils.isEmpty(companyDisplayName)
                                ? companyDisplayName
                                : getString(R.string.admin));
                    }

                    if (tvPickupAddress != null) {
                        tvPickupAddress.setText(!TextUtils.isEmpty(companyAddress)
                                ? companyAddress
                                : getString(R.string.pickup_address_unavailable));
                    }

                    if (btnOpenMaps != null) {
                        boolean hasLocation = companyLocation != null || !TextUtils.isEmpty(companyAddress);
                        btnOpenMaps.setEnabled(hasLocation);
                        btnOpenMaps.setAlpha(hasLocation ? 1f : 0.6f);
                    }

                    companyLocationCard.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to load company location", e));
    }

    private void openDialer(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, R.string.no_phone_available, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_phone_available, Toast.LENGTH_SHORT).show();
        }
    }

    private void openLocationInMaps() {
        if (companyLocation == null && TextUtils.isEmpty(companyAddress)) {
            Toast.makeText(this, R.string.location_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        String label = !TextUtils.isEmpty(companyDisplayName)
                ? companyDisplayName
                : companyAddress;
        if (TextUtils.isEmpty(label)) {
            label = getString(R.string.pickup_location_title);
        }

        String geoUri;
        if (companyLocation != null) {
            double latitude = companyLocation.getLatitude();
            double longitude = companyLocation.getLongitude();
            geoUri = String.format(Locale.ENGLISH,
                    "geo:%f,%f?q=%f,%f(%s)",
                    latitude,
                    longitude,
                    latitude,
                    longitude,
                    Uri.encode(label));
        } else {
            geoUri = "geo:0,0?q=" + Uri.encode(companyAddress);
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            String webUri;
            if (companyLocation != null) {
                double latitude = companyLocation.getLatitude();
                double longitude = companyLocation.getLongitude();
                webUri = String.format(Locale.ENGLISH,
                        "https://www.openstreetmap.org/?mlat=%f&mlon=%f#map=16/%f/%f",
                        latitude,
                        longitude,
                        latitude,
                        longitude);
            } else {
                webUri = "https://www.openstreetmap.org/search?query=" + Uri.encode(companyAddress);
            }
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
            try {
                startActivity(webIntent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, R.string.maps_app_not_found, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to show date picker dialog
    private void showDatePicker(boolean isStartDate) {
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        List<CalendarConstraints.DateValidator> validators = new ArrayList<>();
        validators.add(DateValidatorPointForward.now());

        Long initialSelection = null;
        Long enforcedEarliestSelection = null;
        String selectedText = (isStartDate ? tvStartDate : tvEndDate).getText().toString();
        String defaultText = getString(isStartDate ? R.string.select_start_date : R.string.select_end_date);

        if (!selectedText.equals(defaultText)) {
            try {
                Calendar selectedCal = Calendar.getInstance();
                selectedCal.setTime(Objects.requireNonNull(dateFormat.parse(selectedText)));
                initialSelection = toUtcMidnight(selectedCal);
                constraintsBuilder.setOpenAt(initialSelection);
            } catch (ParseException e) {
                Log.w(TAG, "Unable to parse previously selected date", e);
            }
        }

        if (!isStartDate) {
            String startText = tvStartDate.getText().toString();
            if (!startText.equals(getString(R.string.select_start_date))) {
                try {
                    Calendar startCal = Calendar.getInstance();
                    startCal.setTime(Objects.requireNonNull(dateFormat.parse(startText)));
                    long startUtc = toUtcMidnight(startCal) + TimeUnit.DAYS.toMillis(1);
                    validators.add(DateValidatorPointForward.from(startUtc));
                    enforcedEarliestSelection = startUtc;
                    if (initialSelection == null) {
                        constraintsBuilder.setOpenAt(startUtc);
                    }
                } catch (ParseException e) {
                    Log.w(TAG, "Unable to parse start date for constraints", e);
                }
            }
        }

        constraintsBuilder.setValidator(CompositeDateValidator.allOf(validators));

        long selectionToUse = MaterialDatePicker.todayInUtcMilliseconds();
        if (initialSelection != null) {
            selectionToUse = initialSelection;
        } else if (enforcedEarliestSelection != null && selectionToUse < enforcedEarliestSelection) {
            selectionToUse = enforcedEarliestSelection;
        }

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStartDate ? R.string.select_start_date : R.string.select_end_date)
                .setCalendarConstraints(constraintsBuilder.build())
                .setSelection(selectionToUse)
                .setTheme(R.style.ThemeOverlay_CarRentingTest_DatePicker)
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }

            Calendar chosenDate = fromUtcSelection(selection);
            String formattedDate = dateFormat.format(chosenDate.getTime());

            if (isStartDate) {
                tvStartDate.setText(formattedDate);
                resetEndDateIfBeforeStart();
            } else {
                tvEndDate.setText(formattedDate);
            }
        });

        datePicker.show(getSupportFragmentManager(), isStartDate ? "START_DATE_PICKER" : "END_DATE_PICKER");
    }

    private void resetEndDateIfBeforeStart() {
        String endText = tvEndDate.getText().toString();
        if (endText.equals(getString(R.string.select_end_date))) {
            return;
        }

        try {
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(Objects.requireNonNull(dateFormat.parse(tvStartDate.getText().toString())));
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(Objects.requireNonNull(dateFormat.parse(endText)));

            if (!endCal.after(startCal)) {
                tvEndDate.setText(R.string.select_end_date);
            }
        } catch (ParseException e) {
            Log.w(TAG, "Unable to compare dates when resetting end date", e);
            tvEndDate.setText(R.string.select_end_date);
        }
    }

    private long toUtcMidnight(Calendar localCalendar) {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.clear();
        utcCalendar.set(localCalendar.get(Calendar.YEAR), localCalendar.get(Calendar.MONTH),
                localCalendar.get(Calendar.DAY_OF_MONTH));
        return utcCalendar.getTimeInMillis();
    }

    private Calendar fromUtcSelection(long utcSelection) {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.setTimeInMillis(utcSelection);

        Calendar localCalendar = Calendar.getInstance();
        localCalendar.clear();
        localCalendar.set(utcCalendar.get(Calendar.YEAR), utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH));
        return localCalendar;
    }

    // Method to validate inputs and fetch user's driver license
    private void validateAndFetchLicense() {
        // Get the selected dates
        String startDateStr = tvStartDate.getText().toString();
        String endDateStr = tvEndDate.getText().toString();

        // Check if dates were selected
        if (startDateStr.equals(getString(R.string.select_start_date))
                || endDateStr.equals(getString(R.string.select_end_date))) {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parse the dates into Calendar objects
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();
            startCal.setTime(Objects.requireNonNull(dateFormat.parse(startDateStr)));
            endCal.setTime(Objects.requireNonNull(dateFormat.parse(endDateStr)));

            // Validate that end date is after start date
            if (endCal.before(startCal) || endCal.equals(startCal)) {
                Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if user is logged in
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable submit button to prevent multiple submissions
            findViewById(R.id.btnSubmitRequest).setEnabled(false);
            Toast.makeText(this, "Submitting request...", Toast.LENGTH_SHORT).show();

            // Fetch user data before submitting request
            fetchUserDataAndSubmit(startCal, endCal);

        } catch (Exception e) {
            // Handle date parsing errors
            Log.e(TAG, "Date parsing error: ", e);
            findViewById(R.id.btnSubmitRequest).setEnabled(true);
            Toast.makeText(this, "Invalid date format. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to fetch user data from shared data/domain layers
    private void fetchUserDataAndSubmit(Calendar startCal, Calendar endCal) {
        submitRentalRequestUseCase.execute(
                        selectedCar,
                        companyId,
                        etAdditionalRequests.getText().toString().trim(),
                        startCal.getTime(),
                        endCal.getTime())
                .addOnSuccessListener(request -> showSuccessDialog())
                .addOnFailureListener(e -> {
                    findViewById(R.id.btnSubmitRequest).setEnabled(true);
                    if (SubmitRentalRequestUseCase.isVerificationRequired(e)) {
                        com.google.firebase.analytics.FirebaseAnalytics.getInstance(this)
                                .logEvent("booking_blocked_unverified", new android.os.Bundle());
                        showVerificationRequiredDialog();
                        return;
                    }
Log.w(TAG, "Failed to submit rental request", e);
                    String message = e.getMessage();
                    Toast.makeText(this, message != null ? message : "Failed to submit request", Toast.LENGTH_SHORT).show();
                });
    }

    private void showVerificationRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.modal_title_quick_safety))
                .setMessage(getString(R.string.modal_body_verify_once))
                .setPositiveButton(getString(R.string.modal_button_verify_now), (d, which) -> {
                    NavUtils.openProfileForVerification(this);
                })
                .setNegativeButton(getString(R.string.modal_button_cancel), (d, which) -> d.dismiss())
                .show();
    }

    private void showSuccessDialog() {
        if (isFinishing())
            return;

        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_payment_success,
                null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Set transparent background for rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.btnDone).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        if (vpCarImages != null && imagePageChangeCallback != null) {
            vpCarImages.unregisterOnPageChangeCallback(imagePageChangeCallback);
        }
        super.onDestroy();
    }
}
