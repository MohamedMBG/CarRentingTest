package com.example.carrentingtest.ui.ai;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.R;
import com.example.carrentingtest.utils.GeminiHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ConciergeActivity extends AppCompatActivity {

    private GeminiHelper geminiHelper;
    private EditText etQuery;
    private TextView txtUserQuery;
    private TextView txtAiResponse;
    private FloatingActionButton btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_concierge);

        geminiHelper = new GeminiHelper();

        etQuery = findViewById(R.id.etQuery);
        txtUserQuery = findViewById(R.id.txtUserQuery);
        txtAiResponse = findViewById(R.id.txtAiResponse);
        btnSend = findViewById(R.id.btnSend);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String query = etQuery.getText().toString().trim();
            if (!query.isEmpty()) {
                sendQuery(query);
            }
        });
    }

    private void sendQuery(String query) {
        // Show user query
        txtUserQuery.setText(query);
        txtUserQuery.setVisibility(View.VISIBLE);
        etQuery.setText("");

        // Show loading state
        txtAiResponse.setText("Thinking...");
        txtAiResponse.setVisibility(View.VISIBLE);

        geminiHelper.generateRecommendation(query, new GeminiHelper.RecommendationCallback() {
            @Override
            public void onSuccess(String result) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    txtAiResponse.setText(result);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    txtAiResponse.setText("Sorry, I'm having trouble connecting right now. Please try again.");
                    t.printStackTrace();
                });
            }
        });
    }
}
