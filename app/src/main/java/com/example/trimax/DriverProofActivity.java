package com.example.trimax;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class DriverProofActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;
    private LocationHelper locationHelper;

    private String deliveryCode;
    private Delivery currentDelivery;
    private boolean photoCaptured = false;
    private String capturedPhotoPath = "";
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_proof);

        // Keep screen content clear of the status and navigation bars.
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
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
        TextView tvProofCustomerInfo = findViewById(R.id.tvProofCustomerInfo);
        TextInputEditText etRecipientName = findViewById(R.id.etRecipientName);
        TextInputEditText etOtpCode = findViewById(R.id.etOtpCode);
        CardView cardCapturePhoto = findViewById(R.id.cardCapturePhoto);
        TextView tvPhotoLabel = findViewById(R.id.tvPhotoLabel);
        MaterialButton btnConfirmDelivery = findViewById(R.id.btnConfirmDelivery);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        confirmButton = btnConfirmDelivery;
        confirmLabel = btnConfirmDelivery != null ? btnConfirmDelivery.getText() : "OK";
        if (btnConfirmDelivery != null) btnConfirmDelivery.setEnabled(false);   // until the server says ARRIVED
        loadFromServer(tvProofCustomerInfo, etRecipientName);

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchProofCamera();
                    } else {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Camera Permission Required")
                                .setMessage("Camera access is required to capture delivery proof. Enable Camera permission in App Settings to continue.")
                                .setPositiveButton("Open Settings", (dialog, which) -> {
                                    Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:" + getPackageName()));
                                    startActivity(settingsIntent);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                }
        );

        // Camera Launcher Setup with RESULT_OK validation and Timestamp Overlay
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Bitmap photoBitmap = null;
                        if (result.getData() != null && result.getData().getExtras() != null) {
                            photoBitmap = (Bitmap) result.getData().getExtras().get("data");
                        }

                        if (photoBitmap != null) {
                            File savedFile = ImageTimestampHelper.saveBitmapWithTimestamp(
                                    DriverProofActivity.this,
                                    photoBitmap,
                                    "DELIVERY_PROOF_" + deliveryCode,
                                    locationHelper.getLatitude(),
                                    locationHelper.getLongitude()
                            );

                            if (savedFile != null && savedFile.exists() && savedFile.length() > 0) {
                                photoCaptured = true;
                                capturedPhotoPath = savedFile.getAbsolutePath();

                                if (tvPhotoLabel != null) {
                                    tvPhotoLabel.setText("✓ Delivery Photo Evidence Captured & Timestamped!");
                                    tvPhotoLabel.setTextColor(0xFF059669); // Green
                                }
                                Toast.makeText(this, "Camera Photo Evidence Saved with Timestamp Overlay!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to save photo file to storage.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Failed to retrieve photo data from camera.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Photo capture cancelled or failed.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        if (cardCapturePhoto != null) {
            cardCapturePhoto.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    launchProofCamera();
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            });
        }

        if (btnConfirmDelivery != null) {
            btnConfirmDelivery.setOnClickListener(v -> confirmDelivered());
        }
    }

    // =====================================================================================
    // Server: the delivery must be ARRIVED and assigned to this driver; DELIVERED completes the booking.
    // The photo stays on this phone as evidence (it is not uploaded yet). OTP verification is not
    // implemented on the server, so it is not asked for; the recipient's name is required instead.
    // =====================================================================================

    private BookingRecord currentRecord;
    private MaterialButton confirmButton;
    private CharSequence confirmLabel;
    private boolean submitting;

    private int bookingNumber() {
        try {
            return Integer.parseInt(deliveryCode.replace("#", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void loadFromServer(TextView tvProofCustomerInfo, TextInputEditText etRecipientName) {
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
                currentDelivery = record.toDelivery(true);
                if (tvProofCustomerInfo != null) {
                    tvProofCustomerInfo.setText(String.format("Booking #%s • Customer: %s", record.idText(), record.customerName));
                }
                if (etRecipientName != null) etRecipientName.setText(record.customerName);
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
        if (confirmButton == null || currentRecord == null) return;
        String code = currentRecord.deliveryStatus;
        if (DeliveryProgress.DELIVERED.equals(code) || DeliveryProgress.COMPLETED.equals(currentRecord.status)) {
            confirmButton.setEnabled(false);
            confirmButton.setText("DELIVERY COMPLETED");
        } else if (!DeliveryProgress.ARRIVED.equals(code) || DeliveryProgress.CANCELLED.equals(currentRecord.status)) {
            confirmButton.setEnabled(false);
            confirmButton.setText("MARK ARRIVED FIRST");
        } else {
            confirmButton.setText(confirmLabel);
            confirmButton.setEnabled(!submitting);
        }
    }

    private void confirmDelivered() {
        if (currentRecord == null || submitting) return;
        if (!photoCaptured) {
            Toast.makeText(this, "Take the drop-off photo first.", Toast.LENGTH_LONG).show();
            return;
        }
        final android.widget.EditText recipient = new android.widget.EditText(this);
        recipient.setHint("Name of the person who received it");
        recipient.setText(currentRecord.customerName != null ? currentRecord.customerName : "");
        recipient.setSingleLine(true);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout box = new android.widget.FrameLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(recipient);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Who received the delivery?")
                .setView(box)
                .setPositiveButton("Confirm delivered", (d, w) -> {
                    String name = recipient.getText() != null ? recipient.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Enter the recipient's name.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    submitDelivered(name.length() > 100 ? name.substring(0, 100) : name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitDelivered(String recipientName) {
        submitting = true;
        confirmButton.setEnabled(false);
        confirmButton.setText("SAVING...");
        Double lat = locationHelper.hasValidLocationFix() ? locationHelper.getLatitude() : null;
        Double lng = locationHelper.hasValidLocationFix() ? locationHelper.getLongitude() : null;
        TrimaxApi.get(this).updateDeliveryStatus(currentRecord.bookingId, DeliveryProgress.ARRIVED, DeliveryProgress.DELIVERED,
                lat, lng, null, recipientName, new TrimaxApi.Callback<BookingRecord>() {
                    @Override
                    public void onSuccess(BookingRecord saved) {
                        submitting = false;
                        if (SessionGuard.isGone(DriverProofActivity.this)) return;
                        currentRecord = saved;
                        if (capturedPhotoPath != null && !capturedPhotoPath.isEmpty()) {
                            dbHelper.saveDeliveryProof(saved.idText(), session.getUsername(), capturedPhotoPath, recipientName,
                                    "", "NOT_VERIFIED", locationHelper.getLatitude(), locationHelper.getLongitude());
                        }
                        Toast.makeText(DriverProofActivity.this, "Booking #" + saved.idText() + " delivered and completed.",
                                Toast.LENGTH_LONG).show();
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
                        confirmButton.setText(confirmLabel);
                        Toast.makeText(DriverProofActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                        if (error.getKind() == ApiException.Kind.CONFLICT) {
                            loadFromServer(null, null);
                        } else {
                            updateConfirmButton();
                        }
                    }
                });
    }

    private void launchProofCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to launch camera app on this device.", Toast.LENGTH_SHORT).show();
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
