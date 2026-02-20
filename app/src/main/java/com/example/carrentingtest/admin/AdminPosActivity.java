package com.example.carrentingtest.admin;

import static android.content.ContentValues.TAG;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentingtest.R;
import com.example.carrentingtest.adapters.PosCarAdapter;
import com.example.carrentingtest.models.Car;
import com.example.carrentingtest.models.RentalRequest;
import com.example.carrentingtest.utils.FullscreenUiHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AdminPosActivity extends AppCompatActivity implements PosCarAdapter.PosActionListener {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private TextView tvTotalProfit;
    private CircularProgressIndicator progressIndicator;
    private PosCarAdapter adapter;
    private final List<PosCarAdapter.PosCarSummary> summaries = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private NumberFormat currencyFormat;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String companyId;

    private PosCarAdapter.PosRentalDisplay pendingInvoiceRental;
    private ActivityResultLauncher<Intent> invoiceLauncher;

    private ActivityResultLauncher<String> paymentProofPicker;
    private PosCarAdapter.PosRentalDisplay pendingPaymentProofRental;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_pos);
        FullscreenUiHelper.apply(this, R.id.admin_pos_root);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerPosCars);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvTotalProfit = findViewById(R.id.tvTotalProfit);
        progressIndicator = findViewById(R.id.progressIndicator);

        currencyFormat = buildCurrencyFormat();

        adapter = new PosCarAdapter(summaries, this, currencyFormat, dateFormat);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initInvoiceLauncher();
        initPaymentProofPicker();

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

    private void initPaymentProofPicker() {
        paymentProofPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (pendingPaymentProofRental == null) return;
            if (uri != null) {
                uploadPaymentProof(pendingPaymentProofRental, uri);
            }
            pendingPaymentProofRental = null;
        });
    }

    private void initInvoiceLauncher() {
        invoiceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && pendingInvoiceRental != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    writeInvoice(uri, pendingInvoiceRental);
                }
                pendingInvoiceRental = null;
            } else {
                pendingInvoiceRental = null;
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
                                double ppd = car.getPricePerDay(); // assume primitive
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
                            rentalDisplay.setPaymentProofProvided(request.isPaymentProofProvided());
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
        if (show) {
            progressIndicator.setIndeterminate(true);
            progressIndicator.setVisibility(View.VISIBLE);
        } else {
            progressIndicator.setVisibility(View.GONE);
        }
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
    public void onUploadPaymentProof(@NonNull PosCarAdapter.PosRentalDisplay rental) {
        if (TextUtils.isEmpty(rental.getRequestId())) {
            Toast.makeText(this, R.string.pos_payment_proof_status_update_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        pendingPaymentProofRental = rental;
        if (paymentProofPicker != null) {
            paymentProofPicker.launch("image/*");
        }
    }

    @Override
    public void onRemovePaymentProof(@NonNull PosCarAdapter.PosRentalDisplay rental) {
        if (TextUtils.isEmpty(rental.getRequestId())) {
            Toast.makeText(this, R.string.pos_payment_proof_status_update_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.pos_confirm_remove_payment_proof)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> removePaymentProof(rental))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** NEW: actually handles saving proof locally + updating Firestore */
    private void uploadPaymentProof(@NonNull PosCarAdapter.PosRentalDisplay rental, @NonNull Uri uri) {
        showLoading(true);
        final String previousProofUrl = rental.getPaymentProofUrl();

        ioExecutor.execute(() -> {
            Uri localUri;
            try {
                localUri = savePaymentProofLocally(rental.getRequestId(), uri);
            } catch (IOException e) {
                Log.e(TAG, "Failed to store payment proof locally", e);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    showLoading(false);
                    Toast.makeText(this, R.string.pos_payment_proof_upload_failed, Toast.LENGTH_SHORT).show();
                });
                return;
            }

            runOnUiThread(() -> savePaymentProofMetadata(
                    rental,
                    true,
                    (localUri != null ? localUri.toString() : null),
                    previousProofUrl,
                    R.string.pos_payment_proof_upload_success,
                    R.string.pos_payment_proof_upload_failed
            ));
        });
    }

    private void removePaymentProof(@NonNull PosCarAdapter.PosRentalDisplay rental) {
        showLoading(true);
        ioExecutor.execute(() -> {
            boolean deleted = deleteLocalPaymentProofFile(rental.getPaymentProofUrl());
            if (!deleted && !TextUtils.isEmpty(rental.getPaymentProofUrl())) {
                Log.w(TAG, "Failed to delete local payment proof for request " + rental.getRequestId());
            }
            runOnUiThread(() -> savePaymentProofMetadata(
                    rental,
                    false,
                    null,
                    null,
                    R.string.pos_payment_proof_remove_success,
                    R.string.pos_payment_proof_remove_failed
            ));
        });
    }

    private void savePaymentProofMetadata(@NonNull PosCarAdapter.PosRentalDisplay rental,
                                          boolean hasProof,
                                          @Nullable String proofUrl,
                                          @Nullable String previousProofUrl,
                                          @StringRes int successMessage,
                                          @StringRes int failureMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("paymentProofProvided", hasProof);
        updates.put("paymentProofUrl", proofUrl);

        db.collection("rental_requests")
                .document(rental.getRequestId())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    if (isFinishing() || isDestroyed()) return;
                    showLoading(false);

                    if (!TextUtils.isEmpty(previousProofUrl) && !TextUtils.equals(previousProofUrl, proofUrl)) {
                        ioExecutor.execute(() -> deleteLocalPaymentProofFile(previousProofUrl));
                    }

                    rental.setPaymentProofProvided(hasProof);
                    rental.setPaymentProofUrl(proofUrl);
                    adapter.updatePaymentProofDetails(rental.getRequestId(), hasProof, proofUrl);
                    Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    showLoading(false);

                    // Roll back local file if Firestore update failed after saving locally
                    if (hasProof && !TextUtils.isEmpty(proofUrl)) {
                        ioExecutor.execute(() -> deleteLocalPaymentProofFile(proofUrl));
                    }
                    Toast.makeText(this, failureMessage, Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }

    private Uri savePaymentProofLocally(@Nullable String requestId, @NonNull Uri sourceUri) throws IOException {
        ContentResolver resolver = getContentResolver();
        if (resolver == null) {
            throw new IOException("ContentResolver not available");
        }

        String extension = resolveFileExtension(resolver, sourceUri);
        if (TextUtils.isEmpty(extension)) {
            extension = "jpg";
        }

        String sanitizedId = sanitizeFileName(requestId);
        if (TextUtils.isEmpty(sanitizedId)) {
            sanitizedId = "rental";
        }

        File proofsDir = new File(getFilesDir(), "payment_proofs");
        if (!proofsDir.exists() && !proofsDir.mkdirs()) {
            throw new IOException("Failed to create payment proofs directory");
        }

        File destination = new File(proofsDir, sanitizedId + "_" + System.currentTimeMillis() + "." + extension);

        try (InputStream inputStream = resolver.openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(destination)) {
            if (inputStream == null) {
                throw new IOException("Unable to open input stream for payment proof");
            }

            byte[] buffer = new byte[8 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        }

        return Uri.fromFile(destination);
    }

    private String resolveFileExtension(@NonNull ContentResolver resolver, @NonNull Uri uri) {
        String extension = null;

        String mimeType = resolver.getType(uri);
        if (!TextUtils.isEmpty(mimeType)) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        }

        if (TextUtils.isEmpty(extension)) {
            extension = extractExtensionFromName(resolver, uri);
        }

        if (TextUtils.isEmpty(extension) && uri.getPath() != null) {
            String path = uri.getPath();
            int dotIndex = path.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < path.length() - 1) {
                extension = path.substring(dotIndex + 1);
            }
        }

        return extension;
    }

    @Nullable
    private String extractExtensionFromName(@NonNull ContentResolver resolver, @NonNull Uri uri) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (!TextUtils.isEmpty(name)) {
                    int dotIndex = name.lastIndexOf('.');
                    if (dotIndex >= 0 && dotIndex < name.length() - 1) {
                        return name.substring(dotIndex + 1);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to resolve file extension from cursor", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    private boolean deleteLocalPaymentProofFile(@Nullable String proofUrl) {
        if (TextUtils.isEmpty(proofUrl)) return true;
        Uri uri = Uri.parse(proofUrl);
        if (uri == null || uri.getPath() == null) return true;
        if (!"file".equalsIgnoreCase(uri.getScheme())) return true;
        File file = new File(uri.getPath());
        if (!file.exists()) return true;
        return file.delete();
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

    private NumberFormat buildCurrencyFormat() {
        Locale locale = Locale.getDefault();
        try {
            return NumberFormat.getCurrencyInstance(locale);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Failed to resolve currency for locale " + locale + ", falling back to USD", e);
            try {
                return NumberFormat.getCurrencyInstance(Locale.US);
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "Failed to build fallback currency format, using number instance", ex);
                return NumberFormat.getNumberInstance(Locale.US);
            }
        }
    }
}
