package com.example.trimax;

import android.content.Intent;
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
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class DriverProofActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;
    private LocationHelper locationHelper;

    private String deliveryCode;
    private Delivery currentDelivery;
    private boolean photoCaptured = false;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_proof);

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);
        locationHelper = new LocationHelper(this);

        deliveryCode = getIntent().getStringExtra("DELIVERY_ID");
        if (deliveryCode == null || deliveryCode.isEmpty()) {
            deliveryCode = "TRX-1024";
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvProofCustomerInfo = findViewById(R.id.tvProofCustomerInfo);
        TextInputEditText etRecipientName = findViewById(R.id.etRecipientName);
        TextInputEditText etOtpCode = findViewById(R.id.etOtpCode);
        CardView cardCapturePhoto = findViewById(R.id.cardCapturePhoto);
        TextView tvPhotoLabel = findViewById(R.id.tvPhotoLabel);
        MaterialButton btnConfirmDelivery = findViewById(R.id.btnConfirmDelivery);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        currentDelivery = dbHelper.getDeliveryByCode(deliveryCode);
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

        // Camera Launcher Setup with RESULT_OK validation
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        photoCaptured = true;
                        if (tvPhotoLabel != null) {
                            tvPhotoLabel.setText("✓ Delivery Photo Evidence Captured!");
                            tvPhotoLabel.setTextColor(0xFF059669); // Green
                        }
                        Toast.makeText(this, "Camera Photo Evidence Captured!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Photo capture cancelled or failed.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        if (cardCapturePhoto != null) {
            cardCapturePhoto.setOnClickListener(v -> {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    cameraLauncher.launch(takePictureIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to launch camera app on this device.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnConfirmDelivery != null) {
            btnConfirmDelivery.setOnClickListener(v -> {
                if (!photoCaptured) {
                    Toast.makeText(this, "⚠️ Delivery Photo is REQUIRED before completing drop-off!", Toast.LENGTH_LONG).show();
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
                    if (otp.length() < 4) {
                        if (etOtpCode != null) {
                            etOtpCode.setError("OTP code must be at least 4 digits");
                            etOtpCode.requestFocus();
                        }
                        return;
                    }
                    otpStatus = "VERIFIED_OTP_" + otp;
                } else {
                    otpStatus = "UNVERIFIED_NO_OTP";
                }

                btnConfirmDelivery.setEnabled(false);

                // Save proof locally in SQLite
                String photoPath = "proof_photo_" + deliveryCode + "_" + System.currentTimeMillis() + ".jpg";
                boolean saved = dbHelper.saveDeliveryProof(
                        deliveryCode,
                        session.getUsername(),
                        photoPath,
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
}
