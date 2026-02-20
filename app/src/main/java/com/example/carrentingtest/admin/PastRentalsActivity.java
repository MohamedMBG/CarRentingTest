package com.example.carrentingtest.admin;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentingtest.R;
import com.example.carrentingtest.adapters.PastRentalAdapter;
import com.example.carrentingtest.models.Car;
import com.example.carrentingtest.models.RentalRequest;
import com.example.carrentingtest.utils.FullscreenUiHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.Collections;
import java.util.Date;

public class PastRentalsActivity extends AppCompatActivity implements PastRentalAdapter.InvoiceListener {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private View progressBar;
    private PastRentalAdapter adapter;
    private final List<RentalRequest> pastRentals = new ArrayList<>();
    private final Map<String, Car> carMap = new HashMap<>();
    private final Set<String> pendingCarLoads = new HashSet<>();
    private final Set<String> requestedUserIds = new HashSet<>();
    private FirebaseFirestore db;
    private String companyId;
    private ListenerRegistration pastRentalsRegistration;
    private RentalRequest pendingInvoice;
    private ActivityResultLauncher<Intent> invoiceLauncher;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
    private final NumberFormat amountFormat = NumberFormat.getNumberInstance(Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_rentals);
        FullscreenUiHelper.apply(this, R.id.past_rentals_root);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerPastRentals);
        tvEmptyState = findViewById(R.id.tvEmptyPastRentals);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PastRentalAdapter(this, pastRentals, this);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        amountFormat.setMinimumFractionDigits(2);
        amountFormat.setMaximumFractionDigits(2);

        invoiceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && pendingInvoice != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    writeInvoice(uri, pendingInvoice);
                    return;
                }
            }
            pendingInvoice = null;
        });

        if (auth.getCurrentUser() != null) {
            showLoading(true);
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        companyId = doc.getString("companyId");
                        loadCarsThenRentals();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(this, R.string.error_loading_rentals, Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } else {
            finish();
        }
    }

    private void loadCarsThenRentals() {
        if (companyId == null) {
            showLoading(false);
            return;
        }

        db.collection("cars")
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    carMap.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        Car car = doc.toObject(Car.class);
                        if (car == null) {
                            continue;
                        }
                        car.setDocumentId(doc.getId());
                        carMap.put(doc.getId(), car);
                    }
                    loadPastRentals();
                })
                .addOnFailureListener(e -> loadPastRentals());
    }

    private void loadPastRentals() {
        if (companyId == null) {
            showLoading(false);
            return;
        }

        if (pastRentalsRegistration != null) {
            pastRentalsRegistration.remove();
        }

        showLoading(true);
        pastRentalsRegistration = db.collection("rental_requests")
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("status", "completed")
                .addSnapshotListener((snapshot, error) -> {
                    showLoading(false);
                    if (error != null || snapshot == null) {
                        pastRentals.clear();
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                        Toast.makeText(this, R.string.error_loading_rentals, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    pastRentals.clear();
                    for (DocumentSnapshot doc : snapshot) {
                        RentalRequest request = doc.toObject(RentalRequest.class);
                        if (request == null) {
                            continue;
                        }
                        request.setRequestId(doc.getId());
                        enhanceRequestWithCarData(request);
                        pastRentals.add(request);
                    }
                    Collections.sort(pastRentals, (first, second) -> {
                        Date firstDate = first.getCompletedAt();
                        Date secondDate = second.getCompletedAt();
                        if (firstDate == null && secondDate == null) {
                            return 0;
                        }
                        if (firstDate == null) {
                            return 1;
                        }
                        if (secondDate == null) {
                            return -1;
                        }
                        return secondDate.compareTo(firstDate);
                    });
                    adapter.notifyDataSetChanged();
                    resolveMissingClientDetails();
                    updateEmptyState();
                });
    }

    private void resolveMissingClientDetails() {
        for (RentalRequest request : pastRentals) {
            String userId = request.getUserId();
            boolean missingName = TextUtils.isEmpty(request.getUserName());
            boolean missingPhone = TextUtils.isEmpty(request.getUserPhone());
            if (TextUtils.isEmpty(userId) || (!missingName && !missingPhone)) {
                continue;
            }
            if (requestedUserIds.contains(userId)) {
                continue;
            }
            requestedUserIds.add(userId);
            final RentalRequest target = request;
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        requestedUserIds.remove(userId);
                        boolean changed = false;
                        if (TextUtils.isEmpty(target.getUserName())) {
                            String name = userDoc.getString("name");
                            if (!TextUtils.isEmpty(name)) {
                                target.setUserName(name);
                                changed = true;
                            }
                        }
                        if (TextUtils.isEmpty(target.getUserPhone())) {
                            String phone = userDoc.getString("phone");
                            if (!TextUtils.isEmpty(phone)) {
                                target.setUserPhone(phone);
                                changed = true;
                            }
                        }
                        if (changed) {
                            int index = pastRentals.indexOf(target);
                            if (index >= 0) {
                                adapter.notifyItemChanged(index);
                            }
                        }
                    })
                    .addOnFailureListener(e -> requestedUserIds.remove(userId));
        }
    }

    private void enhanceRequestWithCarData(@NonNull RentalRequest request) {
        String carId = request.getCarId();
        if (TextUtils.isEmpty(carId)) {
            return;
        }

        Car car = carMap.get(carId);
        if (car != null) {
            if (TextUtils.isEmpty(request.getCarModel())) {
                request.setCarModel(car.getModel());
            }
            if (request.getTotalPrice() <= 0 && car.getPricePerDay() > 0) {
                int days = computeRentalDays(request.getStartDate(), request.getEndDate());
                double total = car.getPricePerDay() * Math.max(1, days);
                if (!Double.isNaN(total) && !Double.isInfinite(total)) {
                    request.setTotalPrice(total);
                }
            }
            return;
        }

        if (pendingCarLoads.contains(carId)) {
            return;
        }

        pendingCarLoads.add(carId);
        db.collection("cars")
                .document(carId)
                .get()
                .addOnSuccessListener(doc -> {
                    pendingCarLoads.remove(carId);
                    Car fetched = doc.toObject(Car.class);
                    if (fetched == null) {
                        return;
                    }
                    fetched.setDocumentId(doc.getId());
                    carMap.put(doc.getId(), fetched);
                    enhanceRentalsWithCar(doc.getId());
                })
                .addOnFailureListener(e -> pendingCarLoads.remove(carId));
    }

    private void enhanceRentalsWithCar(@NonNull String carId) {
        for (int i = 0; i < pastRentals.size(); i++) {
            RentalRequest request = pastRentals.get(i);
            if (carId.equals(request.getCarId())) {
                enhanceRequestWithCarData(request);
                adapter.notifyItemChanged(i);
            }
        }
    }

    private void updateEmptyState() {
        boolean empty = pastRentals.isEmpty();
        tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onInvoice(@NonNull RentalRequest request) {
        enhanceRequestWithCarData(request);
        pendingInvoice = request;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        String sanitized = sanitizeFileName(request.getCarModel());
        String base = !TextUtils.isEmpty(sanitized) ? sanitized : "invoice";
        intent.putExtra(Intent.EXTRA_TITLE, base + ".pdf");
        invoiceLauncher.launch(intent);
    }

    private void writeInvoice(@NonNull Uri uri, @NonNull RentalRequest request) {
        showLoading(true);
        OutputStream outputStream = null;
        PdfDocument document = null;
        try {
            outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                Toast.makeText(this, R.string.pos_invoice_failed, Toast.LENGTH_SHORT).show();
                showLoading(false);
                pendingInvoice = null;
                return;
            }

            document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTextSize(24f);
            titlePaint.setFakeBoldText(true);

            Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bodyPaint.setColor(Color.BLACK);
            bodyPaint.setTextSize(16f);

            int x = 40;
            int y = 80;

            canvas.drawText(getString(R.string.pos_invoice_title), x, y, titlePaint);
            y += 40;
            canvas.drawText(getString(R.string.pos_invoice_car, safeText(request.getCarModel())), x, y, bodyPaint);
            y += 28;
            canvas.drawText(getString(R.string.pos_invoice_client, safeText(request.getUserName())), x, y, bodyPaint);
            y += 28;
            if (!TextUtils.isEmpty(request.getUserPhone())) {
                canvas.drawText(getString(R.string.pos_invoice_phone, request.getUserPhone()), x, y, bodyPaint);
                y += 28;
            }
            canvas.drawText(
                    getString(
                            R.string.pos_invoice_period,
                            formatDate(request.getStartDate()),
                            formatDate(request.getEndDate())),
                    x,
                    y,
                    bodyPaint);
            y += 28;
            int days = Math.max(1, computeRentalDays(request.getStartDate(), request.getEndDate()));
            canvas.drawText(getString(R.string.pos_invoice_days, days), x, y, bodyPaint);
            y += 28;
            canvas.drawText(getString(R.string.pos_invoice_total, formatAmount(request.getTotalPrice())), x, y, bodyPaint);

            document.finishPage(page);
            document.writeTo(outputStream);
            Toast.makeText(this, R.string.pos_invoice_success, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.pos_invoice_failed, Toast.LENGTH_SHORT).show();
        } finally {
            if (document != null) {
                document.close();
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
            pendingInvoice = null;
            showLoading(false);
        }
    }

    private String sanitizeFileName(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        return value.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private String safeText(@Nullable String value) {
        return TextUtils.isEmpty(value) ? getString(R.string.pos_value_placeholder) : value;
    }

    private String formatDate(@Nullable Date date) {
        if (date == null) {
            return getString(R.string.pos_value_placeholder);
        }
        return dateFormat.format(date);
    }

    private String formatAmount(double amount) {
        double safeAmount = amount;
        if (Double.isNaN(safeAmount) || Double.isInfinite(safeAmount) || safeAmount < 0) {
            safeAmount = 0d;
        }
        return amountFormat.format(safeAmount) + " dhs";
    }

    private int computeRentalDays(@Nullable Date start, @Nullable Date end) {
        if (start == null) {
            return 0;
        }
        long endMillis = (end != null) ? end.getTime() : start.getTime();
        long diff = Math.max(0, endMillis - start.getTime());
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (diff % TimeUnit.DAYS.toMillis(1) != 0) {
            days++;
        }
        return (int) Math.max(1, days);
    }

    @Override
    protected void onDestroy() {
        if (pastRentalsRegistration != null) {
            pastRentalsRegistration.remove();
            pastRentalsRegistration = null;
        }
        super.onDestroy();
    }
}
