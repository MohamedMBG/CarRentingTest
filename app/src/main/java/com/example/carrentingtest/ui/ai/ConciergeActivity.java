package com.example.carrentingtest.ui.ai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.R;
import com.example.carrentingtest.models.Car;
import com.example.carrentingtest.network.BackendClient;
import com.example.carrentingtest.utils.GeminiHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ConciergeActivity extends AppCompatActivity {

    private EditText etQuery;
    private TextView txtUserQuery;
    private TextView txtAiResponse;
    private FloatingActionButton btnSend;

    private FirebaseFirestore db;
    private List<Car> availableCars = new ArrayList<>();
    private final GeminiHelper geminiHelper = new GeminiHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_concierge);

        etQuery = findViewById(R.id.etQuery);
        txtUserQuery = findViewById(R.id.txtUserQuery);
        txtAiResponse = findViewById(R.id.txtAiResponse);
        btnSend = findViewById(R.id.btnSend);

        db = FirebaseFirestore.getInstance();
        fetchInventory();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String query = etQuery.getText().toString().trim();
            if (!query.isEmpty()) {
                sendQuery(query);
            }
        });
    }

    private void fetchInventory() {
        db.collection("cars")
                .whereEqualTo("available", true)
                .whereEqualTo("maintenance", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    availableCars.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Car car = doc.toObject(Car.class);
                        availableCars.add(car);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure if needed
                });
    }

    private void sendQuery(String query) {
        // Show user query
        txtUserQuery.setText(query);
        txtUserQuery.setVisibility(View.VISIBLE);
        etQuery.setText("");

        txtAiResponse.setText("Thinking...");
        txtAiResponse.setVisibility(View.VISIBLE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!BackendClient.isConfigured()) {
                txtAiResponse.setText(matchCarToQuery(query));
                return;
            }

            geminiHelper.generateRecommendation(query, buildInventoryContext(), new GeminiHelper.RecommendationCallback() {
                @Override
                public void onSuccess(String result) {
                    txtAiResponse.setText(result);
                }

                @Override
                public void onFailure(Throwable t) {
                    txtAiResponse.setText(matchCarToQuery(query));
                }
            });
        }, 600);
    }

    private String buildInventoryContext() {
        StringBuilder context = new StringBuilder();
        for (Car car : availableCars) {
            context.append("- ")
                    .append(car.getModel() != null ? car.getModel() : "Unknown model")
                    .append(" | type=")
                    .append(car.getType() != null ? car.getType() : "unknown")
                    .append(" | transmission=")
                    .append(car.getTransmissionType() != null ? car.getTransmissionType() : "unknown")
                    .append(" | seats=")
                    .append(car.getSeats())
                    .append(" | pricePerDay=")
                    .append(car.getPricePerDay())
                    .append('\n');
        }
        return context.toString().trim();
    }

    private String matchCarToQuery(String query) {
        if (availableCars.isEmpty()) {
            return "I'm sorry, we currently have no cars available in our inventory.";
        }

        String lowerQuery = query.toLowerCase();
        Car bestMatch = null;
        int highestScore = -1;

        for (Car car : availableCars) {
            int score = 0;
            String model = (car.getModel() != null) ? car.getModel().toLowerCase() : "";
            String type = (car.getType() != null) ? car.getType().toLowerCase() : "";
            String trans = (car.getTransmissionType() != null) ? car.getTransmissionType().toLowerCase() : "";

            // Check keywords
            if (lowerQuery.contains(type) && !type.isEmpty())
                score += 3;
            if (lowerQuery.contains(trans) && !trans.isEmpty())
                score += 3;
            if (lowerQuery.contains(model) && !model.isEmpty())
                score += 5;

            // Conceptual matching
            if ((lowerQuery.contains("cheap") || lowerQuery.contains("budget")) && car.getPricePerDay() < 50)
                score += 2;
            if ((lowerQuery.contains("fast") || lowerQuery.contains("sports"))
                    && (type.contains("sport") || type.contains("coupe")))
                score += 3;
            if (lowerQuery.contains("family") && car.getSeats() >= 5)
                score += 2;
            if (lowerQuery.contains(String.valueOf(car.getSeats())))
                score += 2; // e.g. "4 seats"

            if (score > highestScore) {
                highestScore = score;
                bestMatch = car;
            }
        }

        // Return best match or fallback to random/first available car
        if (bestMatch == null || highestScore == 0) {
            bestMatch = availableCars.get(0);
        }

        return "Based on your request, I strongly recommend the " + bestMatch.getModel() +
                ". It's an " + bestMatch.getType() + " with " + bestMatch.getTransmissionType() +
                " transmission and " + bestMatch.getSeats() + " seats. It is available for just $" +
                bestMatch.getPricePerDay() + " per day. Would you like to book this vehicle?";
    }
}
