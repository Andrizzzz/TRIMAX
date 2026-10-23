package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewBookingDetailsActivity extends AppCompatActivity {

    private TextInputEditText etNotes;
    private MaterialButton btnContinue, btnBackStep;
    private LinearLayout containerPackageCards;

    private String customerName, customerPhone, customerEmail;
    private String customerAddress;
    private double customerLatitude, customerLongitude;
    private double pickupLat, pickupLng, destLat, destLng;

    private List<ServicePackage> servicePackages;

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
        String bookingDate = intent.getStringExtra("DATE");
        String bookingTime = intent.getStringExtra("TIME");
        customerAddress = intent.getStringExtra("CUSTOMER_ADDRESS");
        if (customerAddress == null || customerAddress.trim().isEmpty()) {
            customerAddress = intent.getStringExtra("ADDRESS");
        }
        if (customerAddress == null) customerAddress = "";
        customerLatitude = intent.getDoubleExtra("CUSTOMER_LAT", 0);
        customerLongitude = intent.getDoubleExtra("CUSTOMER_LNG", 0);

        pickupLat = 14.7826996;
        pickupLng = 121.0475865;
        destLat = intent.getDoubleExtra("DEST_LAT", 0);
        destLng = intent.getDoubleExtra("DEST_LNG", 0);

        ImageView btnBack = findViewById(R.id.btnBack);
        containerPackageCards = findViewById(R.id.containerPackageCards);
        etNotes = findViewById(R.id.etNotes);
        btnContinue = findViewById(R.id.btnContinue);
        btnBackStep = findViewById(R.id.btnBackStep);

        initServicePackages();
        populatePackageCards();

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnBackStep != null) btnBackStep.setOnClickListener(v -> finish());

        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                if (!Double.isFinite(destLat) || !Double.isFinite(destLng)
                        || destLat < -90 || destLat > 90 || destLng < -180 || destLng > 180
                        || (destLat == 0 && destLng == 0)
                        || customerAddress == null || customerAddress.trim().isEmpty()) {
                    android.widget.Toast.makeText(this, "Customer destination is missing. Go back and select a valid location.", android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                StringBuilder serviceBuilder = new StringBuilder();
                double totalPrice = 0;
                int totalQuantity = 0;

                for (ServicePackage pkg : servicePackages) {
                    if (pkg.getQuantity() > 0) {
                        if (serviceBuilder.length() > 0) {
                            serviceBuilder.append(", ");
                        }
                        serviceBuilder.append(pkg.getName()).append(" (x").append(pkg.getQuantity()).append(")");
                        totalPrice += pkg.getStartingPrice() * pkg.getQuantity();
                        totalQuantity += pkg.getQuantity();
                    }
                }

                if (serviceBuilder.length() == 0) {
                    btnContinue.setEnabled(true);
                    android.widget.Toast.makeText(this, "Please select at least 1 package item before continuing.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                String notes = etNotes != null && etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
                btnContinue.setEnabled(false);

                Intent summaryIntent = new Intent(NewBookingDetailsActivity.this, NewBookingSummaryActivity.class);
                summaryIntent.putExtra("CUSTOMER_NAME", customerName);
                summaryIntent.putExtra("CUSTOMER_PHONE", customerPhone);
                summaryIntent.putExtra("CUSTOMER_EMAIL", customerEmail);
                summaryIntent.putExtra("ADDRESS", customerAddress);
                summaryIntent.putExtra("CUSTOMER_ADDRESS", customerAddress);
                summaryIntent.putExtra("SERVICE", serviceBuilder.toString());
                summaryIntent.putExtra("SELECTED_PRICE", totalPrice / Math.max(1, totalQuantity));
                summaryIntent.putExtra("QUANTITY", totalQuantity);
                summaryIntent.putExtra("DATE", bookingDate != null ? bookingDate : "ASAP");
                summaryIntent.putExtra("TIME", bookingTime != null ? bookingTime : "");
                summaryIntent.putExtra("NOTES", notes);
                summaryIntent.putExtra("PICKUP_LAT", pickupLat);
                summaryIntent.putExtra("PICKUP_LNG", pickupLng);
                summaryIntent.putExtra("DEST_LAT", destLat);
                summaryIntent.putExtra("DEST_LNG", destLng);
                startActivity(summaryIntent);
            });
        }
    }

    private void initServicePackages() {
        servicePackages = new ArrayList<>();
        servicePackages.add(new ServicePackage("pkg_coke", "Coke Case (24 Bottles)", "Wholesale delivery of Coca-Cola cases.", 180.00, R.drawable.ic_delivery_van, null, "Up to 15 kg", "40x30x30 cm", "1 day"));
        servicePackages.add(new ServicePackage("pkg_sprite", "Sprite Case (24 Bottles)", "Wholesale delivery of Sprite cases.", 180.00, R.drawable.ic_delivery_van, null, "Up to 15 kg", "40x30x30 cm", "1 day"));
        servicePackages.add(new ServicePackage("pkg_royal", "Royal Case (24 Bottles)", "Wholesale delivery of Royal orange soda cases.", 175.00, R.drawable.ic_delivery_van, null, "Up to 15 kg", "40x30x30 cm", "1 day"));
        servicePackages.add(new ServicePackage("pkg_standard", "Standard Delivery", "For medium-sized packages.", 99.00, R.drawable.ic_delivery_scooter, null, "Up to 5 kg", "50x40x30 cm", "1 day"));
        servicePackages.add(new ServicePackage("pkg_fragile", "Fragile Delivery", "For delicate and high-value items.", 499.00, R.drawable.ic_delivery_scooter, null, "Up to 10 kg", "50x40x30 cm", "1 day"));
    }

    private void populatePackageCards() {
        if (containerPackageCards == null) return;
        containerPackageCards.removeAllViews();

        for (ServicePackage pkg : servicePackages) {
            View cardView = getLayoutInflater().inflate(R.layout.item_booking_package, containerPackageCards, false);
            ImageView ivImage = cardView.findViewById(R.id.ivPackageImage);
            TextView tvName = cardView.findViewById(R.id.tvPackageName);
            TextView tvDesc = cardView.findViewById(R.id.tvPackageDescription);
            TextView tvPrice = cardView.findViewById(R.id.tvPackagePrice);
            TextView tvItemQuantity = cardView.findViewById(R.id.tvItemQuantity);
            MaterialButton btnMinus = cardView.findViewById(R.id.btnMinus);
            MaterialButton btnPlus = cardView.findViewById(R.id.btnPlus);

            if (ivImage != null) ivImage.setImageResource(pkg.getImageResId());
            if (tvName != null) tvName.setText(pkg.getName());
            if (tvDesc != null) tvDesc.setText(pkg.getDescription());
            if (tvPrice != null) tvPrice.setText(String.format(Locale.US, "₱ %.2f", pkg.getStartingPrice()));
            if (tvItemQuantity != null) tvItemQuantity.setText(String.valueOf(pkg.getQuantity()));

            if (btnMinus != null) {
                btnMinus.setOnClickListener(v -> {
                    if (pkg.getQuantity() > 0) {
                        pkg.setQuantity(pkg.getQuantity() - 1);
                        if (tvItemQuantity != null) tvItemQuantity.setText(String.valueOf(pkg.getQuantity()));
                    }
                });
            }

            if (btnPlus != null) {
                btnPlus.setOnClickListener(v -> {
                    pkg.setQuantity(pkg.getQuantity() + 1);
                    if (tvItemQuantity != null) tvItemQuantity.setText(String.valueOf(pkg.getQuantity()));
                });
            }

            containerPackageCards.addView(cardView);
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
