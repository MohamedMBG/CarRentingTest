package com.example.carrentingtest;

import android.app.DatePickerDialog;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RentalFormActivity extends AppCompatActivity {
    private Car selectedCar;
    private EditText etAdditionalRequests;
    private TextView tvStartDate, tvEndDate;
    private ImageView ivCarImage;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rental_form);

        // Get selected car
        selectedCar = (Car) getIntent().getSerializableExtra("selectedCar");

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
                            .placeholder(R.drawable.car_placeholder) // Add a placeholder image
                            .error(R.drawable.car_placeholder) // Add an error image
                            .transform(new RoundedCorners(24))) // Round only top corners
                    .into(ivCarImage);
        } else {
            // Set default car image if no URL provided
            ivCarImage.setImageResource(R.drawable.car_placeholder);
        }
    }

    private void setupClickListeners() {
        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));
        findViewById(R.id.btnSubmitRequest).setOnClickListener(v -> submitRequest());
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar cal = Calendar.getInstance();

        // Set minimum date to today
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

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void submitRequest() {
        // Validate inputs
        if (tvStartDate.getText().toString().equals("Select start date") ||
                tvEndDate.getText().toString().equals("Select end date")) {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parse dates for validation
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();
            startCal.setTime(dateFormat.parse(tvStartDate.getText().toString()));
            endCal.setTime(dateFormat.parse(tvEndDate.getText().toString()));

            // Validate that end date is after start date
            if (endCal.before(startCal) || endCal.equals(startCal)) {
                Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create and populate request
            RentalRequest request = new RentalRequest();
            request.setCarId(selectedCar.getDocumentId());
            request.setCarModel(selectedCar.getModel());
            request.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());
            request.setUserName(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            request.setAdditionalRequests(etAdditionalRequests.getText().toString().trim());
            request.setStatus("pending");
            request.setStartDate(startCal.getTime());
            request.setEndDate(endCal.getTime());

            // Show loading state
            findViewById(R.id.btnSubmitRequest).setEnabled(false);

            // Save to Firestore and set requestId
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("rental_requests")
                    .add(request)
                    .addOnSuccessListener(documentReference -> {
                        // Update with auto-generated ID
                        db.collection("rental_requests").document(documentReference.getId())
                                .update("requestId", documentReference.getId())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Rental request submitted successfully!", Toast.LENGTH_LONG).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    findViewById(R.id.btnSubmitRequest).setEnabled(true);
                                    Toast.makeText(this, "Failed to update request ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        findViewById(R.id.btnSubmitRequest).setEnabled(true);
                        Toast.makeText(this, "Failed to submit request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            findViewById(R.id.btnSubmitRequest).setEnabled(true);
            Toast.makeText(this, "Invalid date format. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}