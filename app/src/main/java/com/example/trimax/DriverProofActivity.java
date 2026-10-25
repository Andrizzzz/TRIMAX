package com.example.trimax;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;

/**
 * Drop-off and proof of delivery (driver). The server must have the delivery started (STARTED, or an
 * older build's in-between step) and assigned to this driver; DELIVERED needs the recipient's name and
 * completes the booking. The drop-off photo is required here, kept on the phone, and sent to the office
 * in the background afterwards (ProofUploader), so a weak signal never blocks completing a delivery.
 * OTP is not checked by the server, so it is not asked for.
 */
public class DriverProofActivity extends AppCompatActivity {

    private static final String STATE_PHOTO = "proof_photo_path";
    private static final String STATE_PENDING = "proof_pending_capture";

    private DatabaseHelper dbHelper;
    private SessionManager session;
    private LocationHelper locationHelper;

    private String deliveryCode;
    private String capturedPhotoPath = "";
    private File pendingCapture;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    private BookingRecord currentRecord;
    private MaterialButton confirmButton;
    private TextView tvCustomerInfo, tvAddress, tvPhotoLabel, tvStatus;
    private ImageView ivPhoto;
    private View photoPlaceholder;
    private TextInputLayout tilRecipient;
    private TextInputEditText etRecipient;
    private boolean submitting;
    private boolean recipientEdited;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_proof);

        // Keep screen content clear of the status and navigation bars (and the keyboard).
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);
        locationHelper = new LocationHelper(this);

        deliveryCode = getIntent().getStringExtra("DELIVERY_ID");
        if (deliveryCode == null || deliveryCode.trim().isEmpty()) {
            Toast.makeText(this, "Missing delivery ID. Reopen this screen from an assigned delivery.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        deliveryCode = deliveryCode.trim();

        ImageView btnBack = findViewById(R.id.btnBack);
        tvCustomerInfo = findViewById(R.id.tvProofCustomerInfo);
        tvAddress = findViewById(R.id.tvProofAddress);
        tvPhotoLabel = findViewById(R.id.tvPhotoLabel);
        tvStatus = findViewById(R.id.tvProofStatus);
        ivPhoto = findViewById(R.id.ivProofPhoto);
        photoPlaceholder = findViewById(R.id.layoutPhotoPlaceholder);
        tilRecipient = findViewById(R.id.tilRecipientName);
        etRecipient = findViewById(R.id.etRecipientName);
        CardView cardCapturePhoto = findViewById(R.id.cardCapturePhoto);
        confirmButton = findViewById(R.id.btnConfirmDelivery);

        if (btnBack != null) btnBack.setOnClickListener(v -> handleBackNavigation());
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        if (savedInstanceState != null) {
            capturedPhotoPath = savedInstanceState.getString(STATE_PHOTO, "");
            String pending = savedInstanceState.getString(STATE_PENDING, null);
            pendingCapture = pending != null ? new File(pending) : null;
        }
        if (etRecipient != null) {
            etRecipient.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    recipientEdited = true;
                    if (tilRecipient != null) tilRecipient.setError(null);
                }
            });
        }

        cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                launchProofCamera();
            } else {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Camera permission needed")
                        .setMessage("The drop-off photo is required to complete a delivery. Allow the camera in App Settings.")
                        .setPositiveButton("Open Settings", (dialog, which) -> startActivity(new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            File raw = pendingCapture;
            pendingCapture = null;
            if (result.getResultCode() != RESULT_OK) {
                deleteQuietly(raw);
                return;
            }
            Bitmap photo = CameraCapture.readUpright(raw);               // full size, when the camera wrote it
            if (photo == null && result.getData() != null && result.getData().getExtras() != null) {
                Object preview = result.getData().getExtras().get("data"); // small preview otherwise
                if (preview instanceof Bitmap) photo = (Bitmap) preview;
            }
            deleteQuietly(raw);
            if (photo == null) {
                Toast.makeText(this, "No photo came back from the camera. Try again.", Toast.LENGTH_LONG).show();
                return;
            }
            File saved = ImageTimestampHelper.saveBitmapWithTimestamp(this, photo, "DELIVERY_PROOF_" + deliveryCode,
                    locationHelper.getLatitude(), locationHelper.getLongitude());
            if (saved == null || !saved.isFile() || saved.length() == 0) {
                Toast.makeText(this, "The photo could not be saved on this phone. Check free space and try again.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (!capturedPhotoPath.isEmpty() && !capturedPhotoPath.equals(saved.getAbsolutePath())) {
                deleteQuietly(new File(capturedPhotoPath));   // retake: only the latest photo is kept
            }
            capturedPhotoPath = saved.getAbsolutePath();
            showPhoto();
            updateConfirmButton();
        });

        if (cardCapturePhoto != null) {
            cardCapturePhoto.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    launchProofCamera();
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            });
        }
        if (confirmButton != null) {
            confirmButton.setEnabled(false);                 // until the server says the delivery is under way
            confirmButton.setOnClickListener(v -> confirmDelivered());
        }

        showPhoto();
        loadFromServer();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PHOTO, capturedPhotoPath);
        if (pendingCapture != null) outState.putString(STATE_PENDING, pendingCapture.getAbsolutePath());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Own GPS listener for this screen (where the photo was taken and the delivery completed).
        if (!locationHelper.isTrackingActive()) locationHelper.startLocationUpdates(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationHelper.stopLocationUpdates();
    }

    // ------------------------------------------------------------------ server

    private int bookingNumber() {
        try {
            return Integer.parseInt(deliveryCode.replace("#", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void loadFromServer() {
        int id = bookingNumber();
        if (id <= 0) {
            Toast.makeText(this, "Delivery not found. Please reopen an assigned delivery.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        TrimaxApi.get(this).getDriverDelivery(id, new TrimaxApi.Callback<BookingRecord>() {
            @Override
            public void onSuccess(BookingRecord record) {
                if (SessionGuard.isGone(DriverProofActivity.this)) return;
                currentRecord = record;
                if (tvCustomerInfo != null) {
                    String customer = record.customerName != null && !record.customerName.trim().isEmpty()
                            ? record.customerName.trim() : "Customer";
                    tvCustomerInfo.setText("Booking #" + record.idText() + " • " + customer);
                }
                String address = record.deliveryAddress != null && !record.deliveryAddress.trim().isEmpty()
                        ? record.deliveryAddress.trim() : record.customerAddress;
                if (tvAddress != null && address != null && !address.trim().isEmpty()) {
                    tvAddress.setText(address.trim());
                    tvAddress.setVisibility(View.VISIBLE);
                }
                if (etRecipient != null && !recipientEdited && record.customerName != null
                        && (etRecipient.getText() == null || etRecipient.getText().length() == 0)) {
                    etRecipient.setText(record.customerName.trim());
                    recipientEdited = false;
                }
                updateConfirmButton();
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(DriverProofActivity.this)) return;
                if (SessionGuard.handle(DriverProofActivity.this, error)) return;
                Toast.makeText(DriverProofActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void updateConfirmButton() {
        if (confirmButton == null) return;
        if (currentRecord == null) {
            confirmButton.setEnabled(false);
            return;
        }
        String code = currentRecord.deliveryStatus;
        String hint = null;
        boolean enabled = false;
        if (DeliveryProgress.DELIVERED.equals(code) || DeliveryProgress.COMPLETED.equals(currentRecord.status)) {
            confirmButton.setText("Delivery completed");
            hint = "This delivery is already completed.";
        } else if (DeliveryProgress.CANCELLED.equals(currentRecord.status)) {
            confirmButton.setText("Cancelled by the admin");
            hint = "The admin cancelled this booking. Do not deliver it.";
        } else if (DeliveryProgress.FAILED.equals(code)) {
            confirmButton.setText("Delivery failed");
            hint = "This delivery was reported as failed. The admin will review it.";
        } else if (!DeliveryProgress.isUnderWay(code)) {
            confirmButton.setText("Start the delivery first");
            hint = "Go back and tap START DELIVERY once the admin has confirmed the booking.";
        } else {
            confirmButton.setText(submitting ? "Saving…" : "Confirm delivered");
            enabled = !submitting;
            if (capturedPhotoPath.isEmpty()) hint = "Take the drop-off photo to continue.";
        }
        confirmButton.setEnabled(enabled);
        if (tvStatus != null) {
            tvStatus.setText(hint == null ? "" : hint);
            tvStatus.setVisibility(hint == null ? View.GONE : View.VISIBLE);
        }
    }

    private void confirmDelivered() {
        if (currentRecord == null || submitting) return;
        if (capturedPhotoPath.isEmpty() || !new File(capturedPhotoPath).isFile()) {
            capturedPhotoPath = "";
            showPhoto();
            Toast.makeText(this, "Take the drop-off photo first.", Toast.LENGTH_LONG).show();
            return;
        }
        String name = etRecipient != null && etRecipient.getText() != null ? etRecipient.getText().toString().trim() : "";
        if (name.isEmpty()) {
            if (tilRecipient != null) tilRecipient.setError("Enter the name of the person who received it");
            if (etRecipient != null) etRecipient.requestFocus();
            return;
        }
        submitDelivered(name.length() > 100 ? name.substring(0, 100) : name);
    }

    private void submitDelivered(String recipientName) {
        submitting = true;
        updateConfirmButton();
        boolean fix = locationHelper.hasValidLocationFix();
        Double lat = fix ? locationHelper.getLatitude() : null;
        Double lng = fix ? locationHelper.getLongitude() : null;
        String expected = currentRecord.deliveryStatus != null ? currentRecord.deliveryStatus : DeliveryProgress.ASSIGNED;
        TrimaxApi.get(this).updateDeliveryStatus(currentRecord.bookingId, expected, DeliveryProgress.DELIVERED,
                lat, lng, null, recipientName, new TrimaxApi.Callback<BookingRecord>() {
                    @Override
                    public void onSuccess(BookingRecord saved) {
                        submitting = false;
                        // Keep the photo on the phone (as before) and send it to the office in the background.
                        dbHelper.saveDeliveryProof(saved.idText(), session.getUsername(), capturedPhotoPath, recipientName,
                                "", "NOT_VERIFIED", fix ? lat : 0, fix ? lng : 0);
                        ProofUploader.queue(getApplicationContext(), saved.bookingId, new File(capturedPhotoPath),
                                recipientName, lat, lng);
                        if (SessionGuard.isGone(DriverProofActivity.this)) return;
                        currentRecord = saved;
                        Toast.makeText(DriverProofActivity.this, "Booking #" + saved.idText()
                                + " delivered and completed. Sending the photo to the office…", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(DriverProofActivity.this, DriverDeliveriesActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(ApiException error) {
                        submitting = false;
                        if (SessionGuard.isGone(DriverProofActivity.this)) return;
                        if (SessionGuard.handle(DriverProofActivity.this, error)) return;
                        Toast.makeText(DriverProofActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                        if (error.getKind() == ApiException.Kind.CONFLICT || error.getKind() == ApiException.Kind.NOT_FOUND) {
                            loadFromServer();          // show what the server has now
                        } else {
                            updateConfirmButton();
                        }
                    }
                });
    }

    // ------------------------------------------------------------------ photo

    private void showPhoto() {
        boolean has = !capturedPhotoPath.isEmpty() && new File(capturedPhotoPath).isFile();
        if (ivPhoto != null) {
            if (has) {
                ivPhoto.setImageURI(null);                 // force a reload after a retake
                ivPhoto.setImageURI(Uri.fromFile(new File(capturedPhotoPath)));
            }
            ivPhoto.setVisibility(has ? View.VISIBLE : View.GONE);
        }
        if (photoPlaceholder != null) photoPlaceholder.setVisibility(has ? View.GONE : View.VISIBLE);
        if (tvPhotoLabel != null) {
            tvPhotoLabel.setText(has ? "✓ Photo taken with time and GPS stamp. Tap the photo to retake it."
                    : "Show the items at the door, with the house or the receiver.");
            tvPhotoLabel.setTextColor(has ? ContextCompat.getColor(this, R.color.status_confirmed)
                    : ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void launchProofCamera() {
        pendingCapture = CameraCapture.newTargetFile(this, "PROOF_" + deliveryCode);
        try {
            cameraLauncher.launch(CameraCapture.intent(this, pendingCapture));
        } catch (Exception e) {
            deleteQuietly(pendingCapture);
            pendingCapture = null;
            Toast.makeText(this, "No camera app is available on this phone.", Toast.LENGTH_SHORT).show();
        }
    }

    private static void deleteQuietly(File f) {
        if (f != null && f.isFile()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    private void handleBackNavigation() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DriverDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
