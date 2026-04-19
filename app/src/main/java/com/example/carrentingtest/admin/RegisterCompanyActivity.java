package com.example.carrentingtest.admin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.carrentingtest.R;
import com.example.carrentingtest.domain.CompanyLifecycleStatus;
import com.example.carrentingtest.domain.UserLifecycleStatus;
import com.example.carrentingtest.utils.FullscreenUiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class RegisterCompanyActivity extends AppCompatActivity {
    private TextInputLayout tilCompanyName, tilCompanyPhone, tilCompanyAddress, tilCompanyLatitude,
            tilCompanyLongitude, tilAdminName, tilAdminEmail,
            tilAdminPhone, tilAdminPassword, tilConfirmPassword;
    private TextInputEditText etCompanyName, etCompanyPhone, etCompanyAddress, etCompanyLatitude,
            etCompanyLongitude, etAdminName, etAdminEmail,
            etAdminPhone, etAdminPassword, etConfirmPassword;
    private MaterialButton btnRegisterCompany;
    private View progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String companyName;
    private String companyPhone;
    private String companyAddress;
    private double companyLatitude;
    private double companyLongitude;
    private boolean hasCompanyCoordinates;
    private String adminName;
    private String email;
    private String password;
    private String adminPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_company);
        FullscreenUiHelper.apply(this, R.id.register_company_root);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tilCompanyName = findViewById(R.id.tilCompanyName);
        tilCompanyPhone = findViewById(R.id.tilCompanyPhone);
        tilCompanyAddress = findViewById(R.id.tilCompanyAddress);
        tilCompanyLatitude = findViewById(R.id.tilCompanyLatitude);
        tilCompanyLongitude = findViewById(R.id.tilCompanyLongitude);
        tilAdminName = findViewById(R.id.tilAdminName);
        tilAdminEmail = findViewById(R.id.tilAdminEmail);
        tilAdminPassword = findViewById(R.id.tilAdminPassword);
        tilAdminPhone = findViewById(R.id.tilAdminPhone);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etCompanyName = findViewById(R.id.etCompanyName);
        etCompanyPhone = findViewById(R.id.etCompanyPhone);
        etCompanyAddress = findViewById(R.id.etCompanyAddress);
        etCompanyLatitude = findViewById(R.id.etCompanyLatitude);
        etCompanyLongitude = findViewById(R.id.etCompanyLongitude);
        etAdminName = findViewById(R.id.etAdminName);
        etAdminEmail = findViewById(R.id.etAdminEmail);
        etAdminPhone = findViewById(R.id.etAdminPhone);
        etAdminPassword = findViewById(R.id.etAdminPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        progressBar = findViewById(R.id.progressBar);
        btnRegisterCompany = findViewById(R.id.btnRegisterCompany);

        btnRegisterCompany.setOnClickListener(v -> {
            clearErrors();
            if (validateInputs()) {
                registerCompany();
            }
        });
    }

    private void registerCompany() {
        toggleLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                String companyId = db.collection("companies").document().getId();

                Map<String, Object> company = new HashMap<>();
                company.put("name", companyName);
                company.put("phone", companyPhone);
                company.put("address", companyAddress);
                if (hasCompanyCoordinates) {
                    company.put("location", new GeoPoint(companyLatitude, companyLongitude));
                }
                company.put("primaryContactName", adminName);
                company.put("primaryContactEmail", email);
                company.put("primaryContactPhone", adminPhone);
                company.put("status", CompanyLifecycleStatus.PENDING_REVIEW.getStorageValue());
                company.put("createdAt", FieldValue.serverTimestamp());
                db.collection("companies").document(companyId).set(company)
                        .addOnSuccessListener(aVoid -> saveAdminUser(companyId, email))
                        .addOnFailureListener(e -> showError(e != null && e.getMessage() != null
                                ? e.getMessage()
                                : getString(R.string.error_registration_failed)));
            } else {
                showError(task.getException() != null && task.getException().getMessage() != null
                        ? task.getException().getMessage()
                        : getString(R.string.error_registration_failed));
            }
        }).addOnFailureListener(e -> showError(e != null && e.getMessage() != null
                ? e.getMessage()
                : getString(R.string.error_registration_failed)));
    }

    private void saveAdminUser(String companyId, String email) {
        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("role", "admin");
        user.put("companyId", companyId);
        user.put("displayName", adminName);
        user.put("phone", adminPhone);
        user.put("status", UserLifecycleStatus.PENDING_COMPANY_APPROVAL.getStorageValue());
        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    toggleLoading(false);
                    showPendingApprovalDialog(companyId);
                })
                .addOnFailureListener(e -> showError(e != null && e.getMessage() != null
                        ? e.getMessage()
                        : getString(R.string.error_registration_failed)));
    }

    private void showError(String message) {
        toggleLoading(false);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private boolean validateInputs() {
        companyName = getText(etCompanyName);
        companyPhone = getText(etCompanyPhone);
        companyAddress = getText(etCompanyAddress);
        String latitudeText = getText(etCompanyLatitude);
        String longitudeText = getText(etCompanyLongitude);
        adminName = getText(etAdminName);
        email = getText(etAdminEmail);
        adminPhone = getText(etAdminPhone);
        password = getText(etAdminPassword);
        String confirmPassword = getText(etConfirmPassword);

        boolean isValid = true;

        if (TextUtils.isEmpty(companyName)) {
            tilCompanyName.setError(getString(R.string.error_company_name_required));
            isValid = false;
        }

        if (!isValidPhone(companyPhone)) {
            tilCompanyPhone.setError(getString(R.string.error_company_phone_invalid));
            isValid = false;
        }

        if (TextUtils.isEmpty(companyAddress) || companyAddress.length() < 6) {
            tilCompanyAddress.setError(getString(R.string.error_company_address_required));
            isValid = false;
        }

        hasCompanyCoordinates = !TextUtils.isEmpty(latitudeText) || !TextUtils.isEmpty(longitudeText);
        if (hasCompanyCoordinates) {
            if (TextUtils.isEmpty(latitudeText) || TextUtils.isEmpty(longitudeText)) {
                tilCompanyLatitude.setError(getString(R.string.error_company_coordinates_pair));
                tilCompanyLongitude.setError(getString(R.string.error_company_coordinates_pair));
                isValid = false;
            } else {
                Double parsedLatitude = parseCoordinate(latitudeText, -90d, 90d);
                if (parsedLatitude == null) {
                    tilCompanyLatitude.setError(getString(R.string.error_company_latitude_invalid));
                    isValid = false;
                } else {
                    companyLatitude = parsedLatitude;
                }

                Double parsedLongitude = parseCoordinate(longitudeText, -180d, 180d);
                if (parsedLongitude == null) {
                    tilCompanyLongitude.setError(getString(R.string.error_company_longitude_invalid));
                    isValid = false;
                } else {
                    companyLongitude = parsedLongitude;
                }
            }
        }

        if (TextUtils.isEmpty(adminName)) {
            tilAdminName.setError(getString(R.string.error_admin_name_required));
            isValid = false;
        }

        if (!isValidPhone(adminPhone)) {
            tilAdminPhone.setError(getString(R.string.error_admin_phone_invalid));
            isValid = false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilAdminEmail.setError(getString(R.string.error_email_invalid));
            isValid = false;
        }

        if (!isValidPassword(password)) {
            tilAdminPassword.setError(getString(R.string.error_password_requirements));
            isValid = false;
        }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_mismatch));
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        TextInputLayout[] layouts = new TextInputLayout[]{
                tilCompanyName, tilCompanyPhone, tilCompanyAddress, tilCompanyLatitude, tilCompanyLongitude,
                tilAdminName,
                tilAdminEmail, tilAdminPhone, tilAdminPassword, tilConfirmPassword
        };
        for (TextInputLayout layout : layouts) {
            if (layout != null) {
                layout.setError(null);
                layout.setErrorEnabled(false);
            }
        }
    }

    private boolean isValidPhone(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return false;
        }
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        return digitsOnly.length() >= 7;
    }

    private boolean isValidPassword(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        return value.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private Double parseCoordinate(String value, double min, double max) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < min || parsed > max) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void toggleLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegisterCompany.setEnabled(!loading);
    }

    private void showPendingApprovalDialog(String companyId) {
        FirebaseAuth.getInstance().signOut();
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CarRentingTest_AlertDialog)
                .setTitle(R.string.company_signup_complete_title)
                .setMessage(getString(R.string.company_signup_complete_body, companyId))
                .setCancelable(false)
                .setNeutralButton(R.string.copy_company_code, (dialog, which) -> {
                    ClipboardManager clipboard =
                            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("company_code", companyId));
                        Toast.makeText(this, R.string.company_code_copied, Toast.LENGTH_SHORT).show();
                    }
                    openAdminLogin();
                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> openAdminLogin())
                .show();
    }

    private void openAdminLogin() {
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finishAffinity();
    }
}
