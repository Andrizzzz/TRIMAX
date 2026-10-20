package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class NewBookingDetailsActivity extends AppCompatActivity {

    private TextInputEditText etParcelDescription, etNotes;
    private TextView tvQuantity;
    private MaterialButton btnContinue, btnBackStep;

    private int quantity = 1;

    private String customerName, customerPhone, customerEmail;
    private String customerAddress;
    private double customerLatitude, customerLongitude;
    private double pickupLat, pickupLng, destLat, destLng;
    private String selectedPackageName = "";
    private double selectedPackagePrice = 99.00;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_booking_details);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        Intent intent = getIntent();
        customerName = intent.getStringExtra("CUSTOMER_NAME");
        customerPhone = intent.getStringExtra("CUSTOMER_PHONE");
        customerEmail = intent.getStringExtra("CUSTOMER_EMAIL");
        customerAddress = intent.getStringExtra("CUSTOMER_ADDRESS");
        customerLatitude = intent.getDoubleExtra("CUSTOMER_LAT", 0);
        customerLongitude = intent.getDoubleExtra("CUSTOMER_LNG", 0);

        pickupLat = intent.getDoubleExtra("PICKUP_LAT", 14.8115);
        pickupLng = intent.getDoubleExtra("PICKUP_LNG", 121.0453);
        destLat = intent.getDoubleExtra("DEST_LAT", 14.8150);
        destLng = intent.getDoubleExtra("DEST_LNG", 121.0500);

        selectedPackageName = intent.getStringExtra("SELECTED_PACKAGE");
        if (selectedPackageName == null || selectedPackageName.isEmpty()) {
            selectedPackageName = "Standard Delivery";
        }
        selectedPackagePrice = intent.getDoubleExtra("SELECTED_PRICE", 149.00);

        ImageView btnBack = findViewById(R.id.btnBack);
        etParcelDescription = findViewById(R.id.etParcelDescription);
        tvQuantity = findViewById(R.id.tvQuantity);
        MaterialButton btnMinus = findViewById(R.id.btnMinus);
        MaterialButton btnPlus = findViewById(R.id.btnPlus);
        etNotes = findViewById(R.id.etNotes);
        btnContinue = findViewById(R.id.btnContinue);
        btnBackStep = findViewById(R.id.btnBackStep);

        if (etParcelDescription != null) {
            etParcelDescription.setText(selectedPackageName + " (with accessories)");
        }

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnBackStep != null) btnBackStep.setOnClickListener(v -> finish());

        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> {
                if (quantity > 1) {
                    quantity--;
                    if (tvQuantity != null) tvQuantity.setText(String.valueOf(quantity));
                }
            });
        }

        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                quantity++;
                if (tvQuantity != null) tvQuantity.setText(String.valueOf(quantity));
            });
        }

        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                String parcelDesc = etParcelDescription != null && etParcelDescription.getText() != null
                        ? etParcelDescription.getText().toString().trim() : "Laptop (with accessories)";
                String notes = etNotes != null && etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

                if (parcelDesc.isEmpty()) {
                    parcelDesc = "Standard Parcel";
                }

                btnContinue.setEnabled(false);

                Intent summaryIntent = new Intent(NewBookingDetailsActivity.this, NewBookingSummaryActivity.class);
                summaryIntent.putExtra("CUSTOMER_NAME", customerName);
                summaryIntent.putExtra("CUSTOMER_PHONE", customerPhone);
                summaryIntent.putExtra("CUSTOMER_EMAIL", customerEmail);
                summaryIntent.putExtra("ADDRESS", customerAddress);
                summaryIntent.putExtra("SERVICE", parcelDesc);
                summaryIntent.putExtra("SELECTED_PRICE", selectedPackagePrice);
                summaryIntent.putExtra("QUANTITY", quantity);
                summaryIntent.putExtra("DATE", "July 19, 2026");
                summaryIntent.putExtra("TIME", "10:00 AM");
                summaryIntent.putExtra("NOTES", notes);
                summaryIntent.putExtra("PICKUP_LAT", pickupLat);
                summaryIntent.putExtra("PICKUP_LNG", pickupLng);
                summaryIntent.putExtra("DEST_LAT", destLat);
                summaryIntent.putExtra("DEST_LNG", destLng);
                startActivity(summaryIntent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (btnContinue != null) {
            btnContinue.setEnabled(true);
        }
    }
}
