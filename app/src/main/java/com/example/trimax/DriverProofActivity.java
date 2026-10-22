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

        currentDelivery = dbHelper.getDeliveryByCode(deliveryCode);
        if (currentDelivery == null) {
            Toast.makeText(this, "Delivery not found. Please reopen an assigned delivery.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (currentDelivery != null) {
            if (tvProofCustomerInfo != null) {
                tvProofCustomerInfo.setText(String.format("Booking #%s • Customer: %s", currentDelivery.getBookingId(), currentDelivery.getCustomerName()));
            }
            if (etRecipientName != null) {
                etRecipientName.setText(currentDelivery.getCustomerName());
            }
            if ("DELIVERED".equalsIgnoreCase(currentDelivery.getStatus()) || "COMPLETED".equalsIgnoreCase(currentDelivery.getStatus())) {
                Toast.makeText(this, "Delivery #" + deliveryCode + " is already completed.", Toast.LENGTH_LONG).show();
                if (btnConfirmDelivery != null) {
                    btnConfirmDelivery.setEnabled(false);
                    btnConfirmDelivery.setText("DELIVERY COMPLETED");
                }
            }
        }

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
            btnConfirmDelivery.setOnClickListener(v -> {
                if (!photoCaptured || TextUtils.isEmpty(capturedPhotoPath)) {
                    Toast.makeText(this, "⚠️ Delivery Photo Evidence is REQUIRED before completing drop-off!", Toast.LENGTH_LONG).show();
                    return;
                }

                String recipient = etRecipientName != null && etRecipientName.getText() != null ? etRecipientName.getText().toString().trim() : "";
                String otp = etOtpCode != null && etOtpCode.getText() != null ? etOtpCode.getText().toString().trim() : "";

                if (TextUtils.isEmpty(recipient)) {
                    if (etRecipientName != null) {
                        etRecipientName.setError("Recipient name required");
                        etRecipientName.requestFocus();
                    }
                    return;
                }

                String otpStatus;
                if (!otp.isEmpty()) {
                    // Local development mode: SMS backend unavailable; logging entered code for reference without remote verification claim
                    otpStatus = "DEV_MODE_LOCAL_ENTERED_" + otp;
                } else {
                    otpStatus = "DEV_MODE_UNVERIFIED_NO_SMS_BACKEND";
                }

                btnConfirmDelivery.setEnabled(false);

                // Save proof locally in SQLite
                boolean saved = dbHelper.saveDeliveryProof(
                        deliveryCode,
                        session.getUsername(),
                        capturedPhotoPath,
                        recipient,
                        "TOKEN_SIGNED",
                        otpStatus,
                        locationHelper.getLatitude(),
                        locationHelper.getLongitude()
                );

                if (saved) {
                    Toast.makeText(this, "✓ Delivery #" + deliveryCode + " COMPLETED!", Toast.LENGTH_SHORT).show();

                    Delivery nextDelivery = dbHelper.getNextAssignedDelivery(session.getUsername());
                    if (nextDelivery != null) {
                        Toast.makeText(this, "NEXT ASSIGNED DELIVERY: " + nextDelivery.getBookingId(), Toast.LENGTH_LONG).show();
                        Intent nextIntent = new Intent(this, DriverDeliveryDetailsActivity.class);
                        nextIntent.putExtra("DELIVERY_ID", nextDelivery.getBookingId());
                        startActivity(nextIntent);
                    } else {
                        Toast.makeText(this, "All assigned deliveries completed! Returning to Dashboard.", Toast.LENGTH_LONG).show();
                        Intent dashIntent = new Intent(this, DriverDashboardActivity.class);
                        dashIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(dashIntent);
                    }
                    finish();
                } else {
                    btnConfirmDelivery.setEnabled(true);
                    Toast.makeText(this, "Failed to save delivery proof in database", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
