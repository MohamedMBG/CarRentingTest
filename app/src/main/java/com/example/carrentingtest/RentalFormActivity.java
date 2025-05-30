package com.example.carrentingtest;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rental_form);

        // Get selected car
        selectedCar = (Car) getIntent().getSerializableExtra("selectedCar");
        ((TextView) findViewById(R.id.tvSelectedCar)).setText(selectedCar.getModel());

        // Initialize views
        etAdditionalRequests = findViewById(R.id.etAdditionalRequests);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);

        // Set click listeners
        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));
        findViewById(R.id.btnSubmitRequest).setOnClickListener(v -> submitRequest());
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            cal.set(y, m, d);
            (isStartDate ? tvStartDate : tvEndDate).setText(dateFormat.format(cal.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void submitRequest() {
        // Validate inputs
        if (tvStartDate.getText().toString().isEmpty() || tvEndDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select dates", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create and populate request
            RentalRequest request = new RentalRequest();
            request.setCarId(selectedCar.getDocumentId());
            request.setCarModel(selectedCar.getModel());
            request.setUserId(FirebaseAuth.getInstance().getCurrentUser().getUid());
            request.setUserName(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            request.setAdditionalRequests(etAdditionalRequests.getText().toString());
            request.setStatus("pending");
            request.setStartDate(dateFormat.parse(tvStartDate.getText().toString()));
            request.setEndDate(dateFormat.parse(tvEndDate.getText().toString()));

            // Save to Firestore and set requestId
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("rental_requests")
                    .add(request)
                    .addOnSuccessListener(documentReference -> {
                        // Update with auto-generated ID
                        db.collection("rental_requests").document(documentReference.getId())
                                .update("requestId", documentReference.getId())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Request submitted!", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Date format error", Toast.LENGTH_SHORT).show();
        }
    }
}