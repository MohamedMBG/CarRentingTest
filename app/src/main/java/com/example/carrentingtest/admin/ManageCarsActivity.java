package com.example.carrentingtest.admin;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;
import com.example.carrentingtest.R;
import com.example.carrentingtest.adapters.CarAdapter;
import com.example.carrentingtest.domain.RentalRequestStatus;
import com.example.carrentingtest.models.Car;
import com.example.carrentingtest.utils.FullscreenUiHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import java.util.*;

public class ManageCarsActivity extends AppCompatActivity {
    // UI Components
    private ListView carsListView;
    // Data storage
    private List<Car> carList = new ArrayList<>();
    // Adapter for ListView
    private CarAdapter carAdapter;
    // Firestore database instance
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private String companyId;
    private ActivityResultLauncher<String> carImagePicker;
    private EditText activeCarImageUrlInput;

    /**
     * Initial setup when activity is created
     * - Sets up ListView and adapter
     * - Configures button click listeners
     * - Loads initial car data
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_cars); // Set the layout for this activity
        FullscreenUiHelper.apply(this, R.id.manage_cars_root);
        carImagePicker = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                this::uploadCarImages);

        // Initialize ListView and its adapter
        carsListView = findViewById(R.id.carsListView);
        carAdapter = new CarAdapter(this, carList);
        carsListView.setAdapter(carAdapter);

        carAdapter.setOnCarActionListener(new CarAdapter.OnCarActionListener() {
            @Override
            public void onEdit(Car car) {
                showCarDialog(car);
            }

            @Override
            public void onDelete(Car car) {
                deleteCar(car);
            }
        });

        // Set click listener for "Add Car" button
        findViewById(R.id.btnAddCar).setOnClickListener(v -> showCarDialog(null));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Set click listener for ListView items (for edit/delete)
        carsListView.setOnItemClickListener((p, v, pos, id) -> showOptionsDialog(carList.get(pos)));

        AdminAccessManager.guardOperationalAccess(this, db, access -> {
            companyId = access.getCompanyId();
            carAdapter.setClientOrAdmin(true);
            loadCars();
        });

    }

    /**
     * Loads car data from Firestore database
     * - Clears existing list
     * - Fetches all documents from 'cars' collection
     * - Updates adapter when data is loaded
     */
    private void loadCars() {
        if (companyId == null)
            return;
        db.collection("cars")
                .whereEqualTo("companyId", companyId)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        carList.clear(); // Clear existing data
                        // Process each document in the query result
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Car car = doc.toObject(Car.class); // Convert document to Car object
                            car.setDocumentId(doc.getId()); // Store document ID for future reference
                            car.setRentalCount(0);
                            carList.add(car); // Add to local list
                        }
                        carAdapter.notifyDataSetChanged(); // Refresh ListView
                        loadRentalCounts();
                    } else {
                        // Show error message if loading fails
                        Toast.makeText(this, "Failed to load cars", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadRentalCounts() {
        if (companyId == null || carList.isEmpty()) {
            return;
        }

        db.collection("rental_requests")
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Integer> counts = new HashMap<>();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        RentalRequestStatus status = RentalRequestStatus.from(doc.getString("status"));
                        if (!status.isRevenueRecognized())
                            continue;
                        String carId = doc.getString("carId");
                        if (TextUtils.isEmpty(carId))
                            continue;
                        counts.put(carId, counts.getOrDefault(carId, 0) + 1);
                    }

                    for (Car car : carList) {
                        int count = counts.getOrDefault(car.getDocumentId(), 0);
                        car.setRentalCount(count);
                    }
                    carAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    for (Car car : carList) {
                        car.setRentalCount(0);
                    }
                    carAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Shows a dialog for adding/editing a car
     * parameter car The car to edit (null for adding new car)
     */
    private void showCarDialog(Car car) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Inflate the custom dialog layout
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_car_form, null);

        // Get references to all form fields
        EditText etModel = view.findViewById(R.id.etModel),
                etType = view.findViewById(R.id.etType),
                etSeats = view.findViewById(R.id.etSeats),
                etPrice = view.findViewById(R.id.etPrice),
                etImageUrl = view.findViewById(R.id.etImageUrl);
        RadioGroup rgTransmission = view.findViewById(R.id.rgTransmission);
        SwitchCompat swAvailable = view.findViewById(R.id.swAvailable);
        SwitchCompat swMaintenance = view.findViewById(R.id.swMaintenance);
        MaterialButton btnUploadCarImages = view.findViewById(R.id.btnUploadCarImages);
        View statusControls = view.findViewById(R.id.statusControls);

        boolean isEdit = car != null; // Determine if we're editing or adding
        statusControls.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        btnUploadCarImages.setOnClickListener(v -> {
            activeCarImageUrlInput = etImageUrl;
            carImagePicker.launch("image/*");
        });

        // If editing, pre-fill the form with existing values
        if (isEdit) {
            etModel.setText(car.getModel());
            etType.setText(car.getType());
            if (car.getSeats() > 0) {
                etSeats.setText(String.valueOf(car.getSeats()));
            }
            etPrice.setText(String.valueOf(car.getPricePerDay()));
            List<String> existingUrls = car.getImageUrls();
            if (!existingUrls.isEmpty()) {
                etImageUrl.setText(TextUtils.join("\n", existingUrls));
            }
            swAvailable.setChecked(car.isAvailable());
            swMaintenance.setChecked(car.isMaintenance());

            String transmission = car.getTransmissionType();
            if (!TextUtils.isEmpty(transmission)) {
                if (transmission.equalsIgnoreCase(getString(R.string.transmission_manual))) {
                    rgTransmission.check(R.id.rbManual);
                } else {
                    rgTransmission.check(R.id.rbAutomatic);
                }
            }
        }
        setupCarFormPreview(view, etModel, etType, etSeats, etPrice, etImageUrl, rgTransmission);

        // Configure dialog buttons and behavior
        AlertDialog dialog = builder.setView(view)
                .setTitle(isEdit ? "Edit Car" : "Add New Car") // Dynamic title
                .setPositiveButton(isEdit ? "Update" : "Save", null)
                .setNegativeButton("Cancel", null) // Cancel button does nothing
                .create();
        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams();
                params.copyFrom(window.getAttributes());
                params.width = WindowManager.LayoutParams.MATCH_PARENT;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(params);
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (!validateCarForm(view, etModel, etType, etSeats, etPrice)) {
                    return;
                }

                    // Create or get the car object to save
                    Car c = isEdit ? car : new Car();
                    // Set all properties from form fields
                    c.setModel(etModel.getText().toString().trim());
                    c.setType(etType.getText().toString().trim());

                    c.setSeats(Integer.parseInt(etSeats.getText().toString().trim()));

                    c.setPricePerDay(Double.parseDouble(etPrice.getText().toString().trim()));
                    c.setImageUrls(parseImageUrls(etImageUrl.getText().toString()));

                    int selectedTransmissionId = rgTransmission.getCheckedRadioButtonId();
                    String transmissionValue = selectedTransmissionId == R.id.rbManual
                            ? getString(R.string.transmission_manual)
                            : getString(R.string.transmission_automatic);
                    c.setTransmissionType(transmissionValue);
                    // Set availability (only for edits, new cars are available by default)
                    if (isEdit) {
                        c.setAvailable(swAvailable.isChecked());
                        c.setMaintenance(swMaintenance.isChecked());
                    } else {
                        c.setAvailable(true);
                        c.setMaintenance(false);
                    }
                    c.setCompanyId(companyId);

                    // Call appropriate save method
                    if (isEdit)
                        updateCar(c);
                    else
                        addCar(c);
                    dialog.dismiss();
            });
        });
        dialog.setOnDismissListener(d -> {
            if (activeCarImageUrlInput == etImageUrl) {
                activeCarImageUrlInput = null;
            }
        });
        dialog.show();
    }

    private void uploadCarImages(List<Uri> imageUris) {
        if (imageUris == null || imageUris.isEmpty()) {
            return;
        }
        if (TextUtils.isEmpty(companyId) || activeCarImageUrlInput == null) {
            Toast.makeText(this, R.string.car_form_upload_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, getString(R.string.car_form_uploading_photos, imageUris.size()), Toast.LENGTH_SHORT).show();
        for (Uri imageUri : imageUris) {
            uploadCarImage(imageUri);
        }
    }

    private void uploadCarImage(Uri imageUri) {
        String contentType = getContentResolver().getType(imageUri);
        if (TextUtils.isEmpty(contentType)) {
            contentType = "image/jpeg";
        }

        String extension = contentType.endsWith("png") ? ".png" : ".jpg";
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;
        StorageReference imageReference = FirebaseStorage.getInstance()
                .getReference()
                .child("car_images")
                .child(companyId)
                .child(fileName);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(contentType)
                .build();

        imageReference.putFile(imageUri, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        if (exception != null) {
                            throw exception;
                        }
                    }
                    return imageReference.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    appendImageUrl(downloadUri.toString());
                    Toast.makeText(this, R.string.car_form_uploaded_photo, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.car_form_upload_failed, Toast.LENGTH_SHORT).show());
    }

    private void appendImageUrl(String imageUrl) {
        if (activeCarImageUrlInput == null || TextUtils.isEmpty(imageUrl)) {
            return;
        }

        String existing = activeCarImageUrlInput.getText().toString().trim();
        String updated = TextUtils.isEmpty(existing)
                ? imageUrl
                : existing + "\n" + imageUrl;
        activeCarImageUrlInput.setText(updated);
        activeCarImageUrlInput.setSelection(updated.length());
    }

    private void setupCarFormPreview(View view,
                                     EditText etModel,
                                     EditText etType,
                                     EditText etSeats,
                                     EditText etPrice,
                                     EditText etImageUrl,
                                     RadioGroup rgTransmission) {
        ImageView ivCarPreview = view.findViewById(R.id.ivCarPreview);
        TextView tvImageCount = view.findViewById(R.id.tvImageCount);
        TextView tvPreviewTitle = view.findViewById(R.id.tvPreviewTitle);
        TextView tvPreviewMeta = view.findViewById(R.id.tvPreviewMeta);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateCarFormPreview(
                        ivCarPreview,
                        tvImageCount,
                        tvPreviewTitle,
                        tvPreviewMeta,
                        etModel,
                        etType,
                        etSeats,
                        etPrice,
                        etImageUrl,
                        rgTransmission);
            }
        };

        etModel.addTextChangedListener(watcher);
        etType.addTextChangedListener(watcher);
        etSeats.addTextChangedListener(watcher);
        etPrice.addTextChangedListener(watcher);
        etImageUrl.addTextChangedListener(watcher);
        rgTransmission.setOnCheckedChangeListener((group, checkedId) -> updateCarFormPreview(
                ivCarPreview,
                tvImageCount,
                tvPreviewTitle,
                tvPreviewMeta,
                etModel,
                etType,
                etSeats,
                etPrice,
                etImageUrl,
                rgTransmission));

        updateCarFormPreview(
                ivCarPreview,
                tvImageCount,
                tvPreviewTitle,
                tvPreviewMeta,
                etModel,
                etType,
                etSeats,
                etPrice,
                etImageUrl,
                rgTransmission);
    }

    private void updateCarFormPreview(ImageView ivCarPreview,
                                      TextView tvImageCount,
                                      TextView tvPreviewTitle,
                                      TextView tvPreviewMeta,
                                      EditText etModel,
                                      EditText etType,
                                      EditText etSeats,
                                      EditText etPrice,
                                      EditText etImageUrl,
                                      RadioGroup rgTransmission) {
        List<String> urls = parseImageUrls(etImageUrl.getText().toString());
        if (urls.isEmpty()) {
            ivCarPreview.setImageResource(R.drawable.car_placeholder);
            tvImageCount.setText(R.string.car_form_no_images);
        } else {
            Glide.with(this)
                    .load(urls.get(0))
                    .placeholder(R.drawable.car_placeholder)
                    .error(R.drawable.car_placeholder)
                    .centerCrop()
                    .into(ivCarPreview);
            tvImageCount.setText(getString(R.string.car_form_image_count, urls.size()));
        }

        String model = etModel.getText().toString().trim();
        tvPreviewTitle.setText(TextUtils.isEmpty(model)
                ? getString(R.string.car_form_preview_model_placeholder)
                : model);

        String type = etType.getText().toString().trim();
        if (TextUtils.isEmpty(type)) {
            type = getString(R.string.vehicle_type_label);
        }

        String seats = etSeats.getText().toString().trim();
        if (TextUtils.isEmpty(seats)) {
            seats = getString(R.string.seats_label);
        } else {
            try {
                int seatCount = Integer.parseInt(seats);
                seats = getResources().getQuantityString(R.plurals.car_seats_count, seatCount, seatCount);
            } catch (NumberFormatException ignored) {
                seats = getString(R.string.seats_label);
            }
        }

        String transmission = rgTransmission.getCheckedRadioButtonId() == R.id.rbManual
                ? getString(R.string.transmission_manual)
                : getString(R.string.transmission_automatic);

        String price = etPrice.getText().toString().trim();
        if (TextUtils.isEmpty(price)) {
            price = getString(R.string.daily_rate_label);
        } else {
            try {
                price = String.format(Locale.US, getString(R.string.price_per_day_format), Double.parseDouble(price));
            } catch (NumberFormatException ignored) {
                price = getString(R.string.daily_rate_label);
            }
        }

        tvPreviewMeta.setText(type + " | " + seats + " | " + transmission + " | " + price);
    }

    private boolean validateCarForm(View view,
                                    EditText etModel,
                                    EditText etType,
                                    EditText etSeats,
                                    EditText etPrice) {
        TextInputLayout tilModel = view.findViewById(R.id.tilModel);
        TextInputLayout tilType = view.findViewById(R.id.tilType);
        TextInputLayout tilSeats = view.findViewById(R.id.tilSeats);
        TextInputLayout tilPrice = view.findViewById(R.id.tilPrice);

        tilModel.setError(null);
        tilType.setError(null);
        tilSeats.setError(null);
        tilPrice.setError(null);

        if (TextUtils.isEmpty(etModel.getText().toString().trim())) {
            tilModel.setError(getString(R.string.error_car_model_required));
            etModel.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(etType.getText().toString().trim())) {
            tilType.setError(getString(R.string.error_car_type_required));
            etType.requestFocus();
            return false;
        }

        String seatsText = etSeats.getText().toString().trim();
        if (TextUtils.isEmpty(seatsText)) {
            tilSeats.setError(getString(R.string.error_car_seats_required));
            etSeats.requestFocus();
            return false;
        }
        try {
            int seats = Integer.parseInt(seatsText);
            if (seats < 1 || seats > 20) {
                tilSeats.setError(getString(R.string.error_car_seats_invalid));
                etSeats.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            tilSeats.setError(getString(R.string.error_car_seats_invalid));
            etSeats.requestFocus();
            return false;
        }

        String priceText = etPrice.getText().toString().trim();
        if (TextUtils.isEmpty(priceText)) {
            tilPrice.setError(getString(R.string.error_car_price_required));
            etPrice.requestFocus();
            return false;
        }
        try {
            double price = Double.parseDouble(priceText);
            if (price <= 0) {
                tilPrice.setError(getString(R.string.error_car_price_invalid));
                etPrice.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            tilPrice.setError(getString(R.string.error_car_price_invalid));
            etPrice.requestFocus();
            return false;
        }

        return true;
    }

    private List<String> parseImageUrls(String rawInput) {
        List<String> urls = new ArrayList<>();
        if (TextUtils.isEmpty(rawInput)) {
            return urls;
        }

        String[] tokens = rawInput.split("[\n,]");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                urls.add(trimmed);
            }
        }

        return urls;
    }

    /**
     * Adds a new car to Firestore database
     * parameter car The car object to add
     */
    private void addCar(Car car) {
        car.setCompanyId(companyId);
        db.collection("cars").add(car).addOnSuccessListener(doc -> {
            // On success: update local data and show confirmation
            car.setDocumentId(doc.getId()); // Store the auto-generated document ID
            carList.add(car); // Add to local list
            carAdapter.notifyDataSetChanged(); // Refresh ListView
            Toast.makeText(this, "Car added", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e ->
        // Show error message if operation fails
        Toast.makeText(this, "Failed to add car", Toast.LENGTH_SHORT).show());
    }

    /**
     * Updates an existing car in Firestore
     * parameter car The car object with updated values
     */
    private void updateCar(Car car) {
        db.collection("cars").document(car.getDocumentId()).set(car)
                .addOnSuccessListener(v -> {
                    // On success: refresh UI and show confirmation
                    carAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Car updated", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e ->
                // Show error message if operation fails
                Toast.makeText(this, "Failed to update car", Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows options dialog for a car (edit/delete)
     * parameter car The car to perform actions on
     */
    private void showOptionsDialog(Car car) {
        new AlertDialog.Builder(this)
                .setTitle("Car Options")
                .setItems(new String[] { "Edit", "Delete", "Cancel" }, (d, which) -> {
                    // Handle option selection
                    if (which == 0)
                        showCarDialog(car); // Edit
                    else if (which == 1)
                        deleteCar(car); // Delete
                    // Cancel (which == 2) does nothing
                }).show();
    }

    /**
     * Shows confirmation dialog and deletes a car if confirmed
     * parameter car The car to delete
     */
    private void deleteCar(Car car) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Delete this car?")
                .setPositiveButton("Delete", (d, w) ->
                // On confirm: delete from Firestore
                db.collection("cars")
                        .document(car.getDocumentId())
                        .delete()
                        .addOnSuccessListener(v -> {
                            // On success: update local data and show confirmation
                            carList.remove(car);
                            carAdapter.notifyDataSetChanged();
                            Toast.makeText(this, "Car deleted", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e ->
                        // Show error message if operation fails
                        Toast.makeText(this, "Failed to delete car", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null) // Cancel button does nothing
                .show();
    }
}
