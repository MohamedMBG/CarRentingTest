// Package declaration for the activity
package com.example.carrentingtest;

// Import all required Android and Firebase libraries
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
    // Tag for logging purposes
    private static final String TAG = "RentalFormActivity";

    // The car being rented
    private Car selectedCar;

    // UI components
    private EditText etAdditionalRequests;
    private TextView tvStartDate, tvEndDate;
    private ImageView ivCarImage;
    private String companyId;


    // Date formatter for displaying dates
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout for this activity
        setContentView(R.layout.activity_rental_form);

        // Initialize Firebase Firestore and Auth instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                    .addOnSuccessListener(doc -> companyId = doc.getString("companyId"));
        }

        // Get the selected car passed from previous activity
        selectedCar = (Car) getIntent().getSerializableExtra("selectedCar");
        // If no car was passed, show error and close activity
        if (selectedCar == null) {
            Toast.makeText(this, "Error: Car details not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize all UI components
        initializeViews();

        // Set up car details in the UI
        setupCarDetails();

        // Set up click listeners for buttons and date pickers
        setupClickListeners();
    }

    // Method to initialize all view references
    private void initializeViews() {
        etAdditionalRequests = findViewById(R.id.etAdditionalRequests);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        ivCarImage = findViewById(R.id.ivCarImage);
    }

    // Method to set up car details in the UI
    private void setupCarDetails() {
        // Set the car model text
        ((TextView) findViewById(R.id.tvSelectedCar)).setText(selectedCar.getModel());

        // Load car image using Glide library with rounded corners
        if (selectedCar.getImageUrl() != null && !selectedCar.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(selectedCar.getImageUrl())  // Load image from URL
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.car_placeholder)  // Show placeholder while loading
                            .error(R.drawable.car_placeholder)  // Show placeholder if error occurs
                            .transform(new RoundedCorners(24)))  // Apply rounded corners
                    .into(ivCarImage);  // Set the loaded image to ImageView
        } else {
            // If no image URL, use placeholder
            ivCarImage.setImageResource(R.drawable.car_placeholder);
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
    }

    // Method to show date picker dialog
    private void showDatePicker(boolean isStartDate) {
        // Get current date
        Calendar cal = Calendar.getInstance();
        // Create date picker dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    // When date is selected, update the calendar and display the formatted date
                    cal.set(year, month, dayOfMonth);
                    String formattedDate = dateFormat.format(cal.getTime());
                    if (isStartDate) {
                        tvStartDate.setText(formattedDate);
                    } else {
                        tvEndDate.setText(formattedDate);
                    }
                },
                cal.get(Calendar.YEAR),  // Initial year
                cal.get(Calendar.MONTH),  // Initial month
                cal.get(Calendar.DAY_OF_MONTH)  // Initial day
        );
        // Set minimum date to today (can't select past dates)
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        // Show the dialog
        datePickerDialog.show();
    }

    // Method to validate inputs and fetch user's driver license
    private void validateAndFetchLicense() {
        // Get the selected dates
        String startDateStr = tvStartDate.getText().toString();
        String endDateStr = tvEndDate.getText().toString();

        // Check if dates were selected
        if (startDateStr.equals("Select start date") || endDateStr.equals("Select end date")) {
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
            fetchUserDataAndSubmit(currentUser, startCal, endCal);

        } catch (Exception e) {
            // Handle date parsing errors
            Log.e(TAG, "Date parsing error: ", e);
            findViewById(R.id.btnSubmitRequest).setEnabled(true);
            Toast.makeText(this, "Invalid date format. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to fetch user data from Firestore
    private void fetchUserDataAndSubmit(FirebaseUser user, Calendar startCal, Calendar endCal) {
        //db.collection("clients").document(user.getUid()).get()
        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        // Get user name and driver license from document
                        String userName = document.getString("name");
                        String userDriverLicense = document.getString("driverLicense");

                        // Use email as fallback if name is not available
                        if (userName == null) {
                            userName = user.getEmail();
                        }

                        // Check if driver license exists
                        if (userDriverLicense == null || userDriverLicense.isEmpty()) {
                            findViewById(R.id.btnSubmitRequest).setEnabled(true);
                            Toast.makeText(this, "Driver license not found in your profile.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Submit the rental request with all collected data
                        submitRequest(user.getUid(), userName, userDriverLicense, startCal, endCal);

                    } else {
                        // Handle Firestore fetch errors
                        findViewById(R.id.btnSubmitRequest).setEnabled(true);
                        Log.w(TAG, "Error getting user document: ", task.getException());
                        Toast.makeText(this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to submit the rental request to Firestore
    private void submitRequest(String userId, String userName, String userDriverLicense, Calendar startCal, Calendar endCal) {
        // Create new rental request object
        RentalRequest request = new RentalRequest();
        // Set all request properties
        request.setCarId(selectedCar.getDocumentId());
        request.setCarModel(selectedCar.getModel());
        request.setUserId(userId);
        request.setUserName(userName);
        request.setUserDriverLicense(userDriverLicense);
        request.setAdditionalRequests(etAdditionalRequests.getText().toString().trim());
        request.setStatus("pending");
        request.setCompanyId(companyId);
        request.setStartDate(startCal.getTime());
        request.setEndDate(endCal.getTime());

        // Add request to Firestore
        db.collection("rental_requests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    // Update the request with its auto-generated ID
                    db.collection("rental_requests").document(documentReference.getId())
                            .update("requestId", documentReference.getId())
                            .addOnSuccessListener(aVoid -> {
                                // Show success message and close activity
                                Toast.makeText(this, "Rental request submitted successfully!", Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Handle update failure
                                findViewById(R.id.btnSubmitRequest).setEnabled(true);
                                Log.e(TAG, "Error updating request ID: ", e);
                                Toast.makeText(this, "Failed to finalize request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    // Handle submission failure
                    findViewById(R.id.btnSubmitRequest).setEnabled(true);
                    Log.e(TAG, "Error submitting request: ", e);
                    Toast.makeText(this, "Failed to submit request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}