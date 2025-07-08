package com.example.carrentingtest.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carrentingtest.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminLoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);

        findViewById(R.id.btnLogin).setOnClickListener(v -> loginAdmin());
        findViewById(R.id.tvRegisterCompany).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterCompanyActivity.class)));
    }

    private void loginAdmin() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    db.collection("users").document(user.getUid()).get()
                            .addOnSuccessListener(doc -> {
                                String role = doc.getString("role");
                                if ("admin".equals(role)) {
                                    startActivity(new Intent(this, AdminDashboardActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(this, "Not an admin account", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to load user", Toast.LENGTH_SHORT).show());
                }
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
