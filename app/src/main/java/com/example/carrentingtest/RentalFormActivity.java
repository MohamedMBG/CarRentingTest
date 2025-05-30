package com.example.carrentingtest;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.carrentingtest.models.Car;
import com.example.carrentingtest.models.RentalRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class RentalFormActivity extends AppCompatActivity {
    private static final String TAG = "RentalFormActivity";
    private Car selectedCar;
    private EditText etAdditionalRequests;
    private TextView tvStartDate, tvEndDate;
    private ImageView ivCarImage;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rental_form);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get selected car
        selectedCar = (Car) getIntent().getSerializableExtra("selectedCar");
        if (selectedCar == null) {
            Toast.makeText(this, "Error: Car details not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Set car details
        setupCarDetails();

        // Set click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        etAdditionalRequests = findViewById(R.id.etAdditionalRequests);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        ivCarImage = findViewById(R.id.ivCarImage);
    }

    private void setupCarDetails() {
        // Set car model
        ((TextView) findViewById(R.id.tvSelectedCar)).setText(selectedCar.getModel());

        // Load car image using Glide
        if (selectedCar.getImageUrl() != null && !selectedCar.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(selectedCar.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.car_placeholder)
                            .error(R.drawable.car_placeholder)
                            .transform(new RoundedCorners(24)))
                    .into(ivCarImage);
        } else {
            ivCarImage.setImageResource(R.drawable.car_placeholder);
        }
    }

    private void setupClickListeners() {
        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));
        findViewById(R.id.btnSubmitRequest).setOnClickListener(v -> validateAndFetchLicense());
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    cal.set(year, month, dayOfMonth);
                    String formattedDate = dateFormat.format(cal.getTime());
                    if (isStartDate) {
                        tvStartDate.setText(formattedDate);
                    } else {
                        tvEndDate.setText(formattedDate);
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void validateAndFetchLicense() {
        // Validate inputs
        String startDateStr = tvStartDate.getText().toString();
        String endDateStr = tvEndDate.getText().toString();

        if (startDateStr.equals("Select start date") || endDateStr.equals("Select end date")) {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();
            startCal.setTime(Objects.requireNonNull(dateFormat.parse(startDateStr)));
            endCal.setTime(Objects.requireNonNull(dateFormat.parse(endDateStr)));

            if (endCal.before(startCal) || endCal.equals(startCal)) {
                Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
                // Redirect to login or handle appropriately
                return;
            }

            // Show loading state
            findViewById(R.id.btnSubmitRequest).setEnabled(false);
            Toast.makeText(this, "Submitting request...", Toast.LENGTH_SHORT).show();

            // Fetch user data (including driver license) before submitting
            fetchUserDataAndSubmit(currentUser, startCal, endCal);

        } catch (Exception e) {
            Log.e(TAG, "Date parsing error: ", e);
            findViewById(R.id.btnSubmitRequest).setEnabled(true);
            Toast.makeText(this, "Invalid date format. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchUserDataAndSubmit(FirebaseUser user, Calendar startCal, Calendar endCal) {
        db.collection("clients").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        String userName = document.getString("name");
                        String userDriverLicense = document.getString("driverLicense");

                        if (userName == null) {
                            userName = user.getEmail(); // Fallback to email if name is missing
                        }

                        if (userDriverLicense == null || userDriverLicense.isEmpty()) {
                            findViewById(R.id.btnSubmitRequest).setEnabled(true);
                            Toast.makeText(this, "Driver license not found in your profile.", Toast.LENGTH_LONG).show();
                            // Optionally prompt user to update profile
                            return;
                        }

                        // Proceed to submit the request with fetched data
                        submitRequest(user.getUid(), userName, userDriverLicense, startCal, endCal);

                    } else {
                        findViewById(R.id.btnSubmitRequest).setEnabled(true);
                        Log.w(TAG, "Error getting user document: ", task.getException());
                        Toast.makeText(this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void submitRequest(String userId, String userName, String userDriverLicense, Calendar startCal, Calendar endCal) {
        // Create and populate request
        RentalRequest request = new RentalRequest();
        request.setCarId(selectedCar.getDocumentId());
        request.setCarModel(selectedCar.getModel());
        request.setUserId(userId);
        request.setUserName(userName); // Use fetched name
        request.setUserDriverLicense(userDriverLicense); // Add fetched driver license
        request.setAdditionalRequests(etAdditionalRequests.getText().toString().trim());
        request.setStatus("pending");
        request.setStartDate(startCal.getTime());
        request.setEndDate(endCal.getTime());

        // Save to Firestore and set requestId
        db.collection("rental_requests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    // Update with auto-generated ID
                    db.collection("rental_requests").document(documentReference.getId())
                            .update("requestId", documentReference.getId())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Rental request submitted successfully!", Toast.LENGTH_LONG).show();
                                finish(); // Close activity after success
                            })
                            .addOnFailureListener(e -> {
                                findViewById(R.id.btnSubmitRequest).setEnabled(true);
                                Log.e(TAG, "Error updating request ID: ", e);
                                Toast.makeText(this, "Failed to finalize request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    findViewById(R.id.btnSubmitRequest).setEnabled(true);
                    Log.e(TAG, "Error submitting request: ", e);
                    Toast.makeText(this, "Failed to submit request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

