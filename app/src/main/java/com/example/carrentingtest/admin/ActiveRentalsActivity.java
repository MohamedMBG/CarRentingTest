package com.example.carrentingtest.admin;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.carrentingtest.R;
import com.example.carrentingtest.models.RentalRequest;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.recyclerview.widget.RecyclerView;

public class ActiveRentalsActivity extends AppCompatActivity implements ActiveRentalAdapter.OnCallClientListener {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private View progressBar;
    private MaterialButton btnExport;
    private ActiveRentalAdapter adapter;
    private final List<RentalRequest> activeRentals = new ArrayList<>();
    private FirebaseFirestore db;
    private String companyId;
    private ActivityResultLauncher<Intent> exportLauncher;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_rentals);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerActiveRentals);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressBar = findViewById(R.id.progressBar);
        btnExport = findViewById(R.id.btnExportExcel);

        adapter = new ActiveRentalAdapter(activeRentals, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        exportLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    writeExcel(uri);
                }
            }
        });

        btnExport.setOnClickListener(v -> promptExport());

        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        companyId = doc.getString("companyId");
                        loadActiveRentals();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, R.string.error_registration_failed, Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } else {
            finish();
        }
    }

    private void loadActiveRentals() {
        if (companyId == null) {
            updateEmptyState();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        db.collection("rental_requests")
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("status", "approved")
                .addSnapshotListener((snapshot, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null || snapshot == null) {
                        Toast.makeText(this, R.string.no_requests_found, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    activeRentals.clear();
                    long now = System.currentTimeMillis();
                    for (DocumentSnapshot doc : snapshot) {
                        RentalRequest request = doc.toObject(RentalRequest.class);
                        if (request == null) {
                            continue;
                        }
                        request.setRequestId(doc.getId());
                        Date endDate = request.getEndDate();
                        if (endDate != null && endDate.getTime() < now) {
                            continue;
                        }
                        activeRentals.add(request);
                    }
                    adapter.notifyDataSetChanged();
                    resolveMissingPhones();
                    updateEmptyState();
                });
    }

    private void resolveMissingPhones() {
        for (RentalRequest request : activeRentals) {
            if (TextUtils.isEmpty(request.getUserPhone()) && !TextUtils.isEmpty(request.getUserId())) {
                db.collection("users")
                        .document(request.getUserId())
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            String phone = userDoc.getString("phone");
                            if (!TextUtils.isEmpty(phone)) {
                                request.setUserPhone(phone);
                                int index = activeRentals.indexOf(request);
                                if (index >= 0) {
                                    adapter.notifyItemChanged(index);
                                } else {
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        });
            }
        }
    }

    private void updateEmptyState() {
        tvEmptyState.setVisibility(activeRentals.isEmpty() ? View.VISIBLE : View.GONE);
        btnExport.setEnabled(!activeRentals.isEmpty());
        btnExport.setAlpha(activeRentals.isEmpty() ? 0.6f : 1f);
    }

    private void promptExport() {
        if (activeRentals.isEmpty()) {
            Toast.makeText(this, R.string.active_rentals_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.export_file_name));
        exportLauncher.launch(intent);
    }

    private void writeExcel(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri);
             Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Active Rentals");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Car Model");
            header.createCell(1).setCellValue("Client Name");
            header.createCell(2).setCellValue("Client Phone");
            header.createCell(3).setCellValue("Start Date");
            header.createCell(4).setCellValue("End Date");
            header.createCell(5).setCellValue("Elapsed");
            header.createCell(6).setCellValue("Remaining");
            header.createCell(7).setCellValue("Status");

            long now = System.currentTimeMillis();
            int rowIndex = 1;
            for (RentalRequest request : activeRentals) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(!TextUtils.isEmpty(request.getCarModel()) ? request.getCarModel() : "-");
                row.createCell(1).setCellValue(!TextUtils.isEmpty(request.getUserName()) ? request.getUserName() : "-");
                row.createCell(2).setCellValue(!TextUtils.isEmpty(request.getUserPhone()) ? request.getUserPhone() : "-");
                row.createCell(3).setCellValue(request.getStartDate() != null ? dateFormat.format(request.getStartDate()) : "-");
                row.createCell(4).setCellValue(request.getEndDate() != null ? dateFormat.format(request.getEndDate()) : "-");
                row.createCell(5).setCellValue(formatDurationForExport(request.getStartDate(), now));
                row.createCell(6).setCellValue(formatRemainingForExport(request.getEndDate(), now));
                row.createCell(7).setCellValue(!TextUtils.isEmpty(request.getStatus()) ? request.getStatus() : "-");
            }

            for (int i = 0; i <= 7; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(os);
            Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDurationForExport(Date startDate, long now) {
        if (startDate == null) {
            return "-";
        }
        long elapsed = Math.max(0, now - startDate.getTime());
        return formatMillis(elapsed);
    }

    private String formatRemainingForExport(Date endDate, long now) {
        if (endDate == null) {
            return "-";
        }
        long remaining = Math.max(0, endDate.getTime() - now);
        return formatMillis(remaining);
    }

    private String formatMillis(long millis) {
        long totalHours = TimeUnit.MILLISECONDS.toHours(millis);
        if (totalHours <= 0) {
            return getString(R.string.hours_suffix, 0);
        }
        long days = totalHours / 24;
        long hours = totalHours % 24;
        if (days > 0) {
            return getString(R.string.days_hours_format, days, hours);
        }
        return getString(R.string.hours_suffix, totalHours);
    }

    @Override
    public void onCallClient(String phoneNumber) {
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
}
