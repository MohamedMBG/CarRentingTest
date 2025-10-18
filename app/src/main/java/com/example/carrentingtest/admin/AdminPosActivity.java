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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentingtest.R;
import com.example.carrentingtest.adapters.PosCarAdapter;
import com.example.carrentingtest.models.Car;
import com.example.carrentingtest.models.RentalRequest;
import com.example.carrentingtest.storage.StoragePaths;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AdminPosActivity extends AppCompatActivity implements PosCarAdapter.PosActionListener {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private TextView tvTotalProfit;
    private CircularProgressIndicator progressIndicator;
    private PosCarAdapter adapter;
    private final List<PosCarAdapter.PosCarSummary> summaries = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;
    private String companyId;

    private PosCarAdapter.PosRentalDisplay pendingInvoiceRental;
    private PosCarAdapter.PosRentalDisplay pendingProofRental;
    private ActivityResultLauncher<Intent> invoiceLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri cameraTempUri;
    private File cameraTempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_pos);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerPosCars);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvTotalProfit = findViewById(R.id.tvTotalProfit);
        progressIndicator = findViewById(R.id.progressIndicator);

        adapter = new PosCarAdapter(summaries, this, currencyFormat, dateFormat);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        initActivityResultLaunchers();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, R.string.error_not_authenticated, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);
        String uid = auth.getCurrentUser().getUid();
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (isFinishing() || isDestroyed()) return;
                    companyId = doc != null ? doc.getString("companyId") : null;
                    if (TextUtils.isEmpty(companyId)) {
                        showLoading(false);
                        Toast.makeText(this, R.string.error_company_not_found, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        loadCars();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    showLoading(false);
                    Toast.makeText(this, R.string.error_company_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearCameraTempFile();
    }

    private void initActivityResultLaunchers() {
        invoiceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && pendingInvoiceRental != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    writeInvoice(uri, pendingInvoiceRental);
                } else {
                    pendingInvoiceRental = null;
                }
            } else {
                pendingInvoiceRental = null;
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && pendingProofRental != null) {
                uploadPaymentProof(uri, false);
            } else {
                pendingProofRental = null;
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraTempUri != null && pendingProofRental != null) {
                uploadPaymentProof(cameraTempUri, true);
            } else {
                clearCameraTempFile();
                pendingProofRental = null;
            }
        });
    }

    private void loadCars() {
        showLoading(true);
        db.collection("cars")
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) return;
                    Map<String, Car> carMap = new HashMap<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot) {
                            Car car = doc.toObject(Car.class);
                            if (car == null) continue;
                            car.setDocumentId(doc.getId());
                            carMap.put(doc.getId(), car);
                        }
                    }
                    loadRentals(carMap);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    showLoading(false);
                    Toast.makeText(this, R.string.error_loading_cars, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRentals(Map<String, Car> carMap) {
        db.collection("rental_requests")
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) return;

                    Map<String, List<PosCarAdapter.PosRentalDisplay>> perCar = new HashMap<>();
                    double totalRevenue = 0d;
                    long now = System.currentTimeMillis();

                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot) {
                            RentalRequest request = doc.toObject(RentalRequest.class);
                            if (request == null) continue;
                            request.setRequestId(doc.getId());

                            String status = request.getStatus();
                            if (TextUtils.isEmpty(status)) continue;
                            if (!"approved".equalsIgnoreCase(status) && !"completed".equalsIgnoreCase(status)) continue;

                            String carId = request.getCarId();
                            if (TextUtils.isEmpty(carId)) continue;

                            Car car = carMap.get(carId);
                            if (car == null) continue;

                            int rentalDays = computeRentalDays(request.getStartDate(), request.getEndDate(), now);

                            double price = request.getTotalPrice();
                            if (price <= 0) {
                                double ppd = car.getPricePerDay(); // primitive, no null check
                                price = ppd * Math.max(1, rentalDays);
                            }
                            if (Double.isNaN(price) || Double.isInfinite(price)) price = 0d;

                            totalRevenue += price;

                            PosCarAdapter.PosRentalDisplay rentalDisplay = new PosCarAdapter.PosRentalDisplay();
                            rentalDisplay.setRequestId(request.getRequestId());
                            rentalDisplay.setCarId(carId);
                            rentalDisplay.setCarModel(safeText(car.getModel()));
                            rentalDisplay.setUserName(safeText(request.getUserName()));
                            rentalDisplay.setUserPhone(request.getUserPhone());
                            rentalDisplay.setStartDate(request.getStartDate());
                            rentalDisplay.setEndDate(request.getEndDate());
                            rentalDisplay.setStatus(status);
                            rentalDisplay.setRentalDays(Math.max(1, rentalDays));
                            rentalDisplay.setTotalPrice(price);
                            rentalDisplay.setPaymentProofUrl(request.getPaymentProofUrl());

                            perCar.computeIfAbsent(carId, k -> new ArrayList<>()).add(rentalDisplay);
                        }
                    }

                    summaries.clear();
                    for (Car car : carMap.values()) {
                        List<PosCarAdapter.PosRentalDisplay> rentals =
                                perCar.getOrDefault(car.getDocumentId(), new ArrayList<>());
                        double revenue = 0d;
                        for (PosCarAdapter.PosRentalDisplay r : rentals) {
                            revenue += r.getTotalPrice();
                        }
                        summaries.add(new PosCarAdapter.PosCarSummary(car, rentals, revenue));
                    }

                    summaries.sort((a, b) -> Double.compare(b.getTotalRevenue(), a.getTotalRevenue()));
                    adapter.updateData(summaries);

                    tvTotalProfit.setText(
                            getString(R.string.pos_total_profit_value, currencyFormat.format(totalRevenue))
                    );
                    updateEmptyState();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    showLoading(false);
                    Toast.makeText(this, R.string.error_loading_rentals, Toast.LENGTH_SHORT).show();
                });
    }

    private int computeRentalDays(Date startDate, Date endDate, long now) {
        if (startDate == null) return 0;
        long endMillis = (endDate != null) ? endDate.getTime() : now;
        // If end before start, clamp to at least 1 day
        long diff = Math.max(0, endMillis - startDate.getTime());
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (diff % TimeUnit.DAYS.toMillis(1) != 0) days++;
        return (int) Math.max(1, days);
    }

    private void updateEmptyState() {
        boolean empty = adapter.getItemCount() == 0;
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showLoading(boolean show) {
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onGenerateInvoice(@NonNull PosCarAdapter.PosRentalDisplay rental) {
        pendingInvoiceRental = rental;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        String sanitizedModel = sanitizeFileName(rental.getCarModel());
        String base = !TextUtils.isEmpty(sanitizedModel) ? sanitizedModel : "invoice";
        intent.putExtra(Intent.EXTRA_TITLE, base + ".pdf");
        invoiceLauncher.launch(intent);
    }

    @Override
    public void onAttachPaymentProof(@NonNull PosCarAdapter.PosRentalDisplay rental) {
        pendingProofRental = rental;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pos_select_proof_source)
                .setItems(new CharSequence[]{getString(R.string.pos_select_from_gallery), getString(R.string.pos_take_photo)}, (dialog, which) -> {
                    if (which == 0) {
                        galleryLauncher.launch("image/*");
                    } else {
                        launchCamera();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> pendingProofRental = null)
                .show();
    }

    @Override
    public void onPreviewProof(@NonNull String proofUrl) {
        try {
            if (TextUtils.isEmpty(proofUrl)) {
                Toast.makeText(this, R.string.pos_preview_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(proofUrl)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.pos_preview_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchCamera() {
        clearCameraTempFile();
        try {
            cameraTempFile = File.createTempFile("payment_proof_", ".jpg", getCacheDir());
            cameraTempUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", cameraTempFile);
            cameraLauncher.launch(cameraTempUri);
        } catch (IOException e) {
            Toast.makeText(this, R.string.pos_take_photo_error, Toast.LENGTH_SHORT).show();
            pendingProofRental = null;
        }
    }

    private void uploadPaymentProof(Uri uri, boolean fromCamera) {
        if (pendingProofRental == null || uri == null) {
            clearCameraTempFile();
            return;
        }

        String requestId = pendingProofRental.getRequestId();
        if (TextUtils.isEmpty(requestId)) {
            Toast.makeText(this, R.string.pos_upload_failed, Toast.LENGTH_SHORT).show();
            pendingProofRental = null;
            if (fromCamera) clearCameraTempFile();
            return;
        }

        showLoading(true);
        StorageReference ref = storage.getReference()
                .child(StoragePaths.paymentProofPath(requestId));

        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> db.collection("rental_requests")
                        .document(pendingProofRental.getRequestId())
                        .update("paymentProofUrl", downloadUri.toString())
                        .addOnSuccessListener(unused -> {
                            if (isFinishing() || isDestroyed()) return;
                            showLoading(false);
                            pendingProofRental.setPaymentProofUrl(downloadUri.toString());
                            adapter.updatePaymentProof(pendingProofRental.getRequestId(), downloadUri.toString());
                            Toast.makeText(this, R.string.pos_upload_success, Toast.LENGTH_SHORT).show();
                            pendingProofRental = null;
                            if (fromCamera) clearCameraTempFile();
                        })
                        .addOnFailureListener(e -> {
                            if (isFinishing() || isDestroyed()) return;
                            showLoading(false);
                            Toast.makeText(this, R.string.pos_upload_failed, Toast.LENGTH_SHORT).show();
                            pendingProofRental = null;
                            if (fromCamera) clearCameraTempFile();
                        }))
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    showLoading(false);
                    Toast.makeText(this, R.string.pos_upload_failed, Toast.LENGTH_SHORT).show();
                    pendingProofRental = null;
                    if (fromCamera) clearCameraTempFile();
                });
    }

    private void writeInvoice(Uri uri, PosCarAdapter.PosRentalDisplay rental) {
        showLoading(true);
        OutputStream os = null;
        PdfDocument document = null;
        try {
            os = getContentResolver().openOutputStream(uri);
            if (os == null) {
                showLoading(false);
                Toast.makeText(this, R.string.pos_invoice_failed, Toast.LENGTH_SHORT).show();
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
            canvas.drawText("Car: " + safeText(rental.getCarModel()), x, y, bodyPaint);
            y += 28;
            canvas.drawText("Client: " + safeText(rental.getUserName()), x, y, bodyPaint);
            y += 28;
            if (!TextUtils.isEmpty(rental.getUserPhone())) {
                canvas.drawText("Phone: " + rental.getUserPhone(), x, y, bodyPaint);
                y += 28;
            }
            canvas.drawText("Period: " + formatDate(rental.getStartDate()) + " — " + formatDate(rental.getEndDate()), x, y, bodyPaint);
            y += 28;
            canvas.drawText("Days: " + rental.getRentalDays(), x, y, bodyPaint);
            y += 28;
            canvas.drawText("Total: " + currencyFormat.format(rental.getTotalPrice()), x, y, bodyPaint);

            document.finishPage(page);
            document.writeTo(os);

            showLoading(false);
            Toast.makeText(this, R.string.pos_invoice_success, Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            showLoading(false);
            Toast.makeText(this, R.string.pos_invoice_failed, Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (document != null) document.close();
                if (os != null) os.close();
            } catch (IOException ignored) {}
            pendingInvoiceRental = null;
        }
    }


    private String safeText(String value) {
        return TextUtils.isEmpty(value) ? getString(R.string.pos_value_placeholder) : value;
    }

    private String formatDate(Date date) {
        return (date == null) ? getString(R.string.pos_value_placeholder) : dateFormat.format(date);
    }

    private String sanitizeFileName(String value) {
        if (TextUtils.isEmpty(value)) return null;
        // keep letters, digits, underscore and dash
        return value.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    private void clearCameraTempFile() {
        if (cameraTempFile != null && cameraTempFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cameraTempFile.delete();
        }
        cameraTempFile = null;
        cameraTempUri = null;
    }
}
