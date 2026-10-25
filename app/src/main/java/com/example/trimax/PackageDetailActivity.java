package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class PackageDetailActivity extends AppCompatActivity {

    private ServicePackage servicePackage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_package_detail);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        servicePackage = (ServicePackage) getIntent().getSerializableExtra("SERVICE_PACKAGE");
        if (servicePackage == null) {
            // Default fallback if opened directly
            servicePackage = new ServicePackage(
                    "pkg_express",
                    "Express Delivery",
                    "For urgent deliveries (available in select areas).",
                    249.00,
                    R.drawable.ic_delivery_van,
                    java.util.Arrays.asList("Priority dispatch", "Safe and secure delivery", "Real-time tracking", "Delivery confirmation"),
                    "Up to 5 kg",
                    "40 x 30 x 20 cm",
                    "2 - 4 hours"
            );
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView ivImage = findViewById(R.id.ivDetailPackageImage);
        TextView tvName = findViewById(R.id.tvDetailPackageName);
        TextView tvPrice = findViewById(R.id.tvDetailPackagePrice);
        TextView tvDesc = findViewById(R.id.tvDetailPackageDescription);
        LinearLayout containerServices = findViewById(R.id.containerIncludedServices);
        TextView tvMaxWeight = findViewById(R.id.tvMaxWeight);
        TextView tvMaxDimensions = findViewById(R.id.tvMaxDimensions);
        TextView tvEstimatedTime = findViewById(R.id.tvEstimatedTime);
        MaterialButton btnSelectPackage = findViewById(R.id.btnSelectPackage);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (ivImage != null) ivImage.setImageResource(servicePackage.getImageResId());
        if (tvName != null) tvName.setText(servicePackage.getName());
        if (tvPrice != null) tvPrice.setText(String.format(Locale.US, "from ₱%.0f", servicePackage.getStartingPrice()));
        if (tvDesc != null) tvDesc.setText(servicePackage.getDescription());
        if (tvMaxWeight != null) tvMaxWeight.setText(servicePackage.getMaxWeight());
        if (tvMaxDimensions != null) tvMaxDimensions.setText(servicePackage.getMaxDimensions());
        if (tvEstimatedTime != null) tvEstimatedTime.setText(servicePackage.getEstimatedTime());

        if (containerServices != null && servicePackage.getIncludedServices() != null) {
            containerServices.removeAllViews();
            for (String service : servicePackage.getIncludedServices()) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(4), 0, dp(4));

                ImageView check = new ImageView(this);
                check.setImageResource(R.drawable.ic_check_circle);
                check.setColorFilter(ContextCompat.getColor(this, R.color.status_confirmed));
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(20), dp(20));
                iconParams.setMarginEnd(dp(12));
                check.setLayoutParams(iconParams);

                TextView tvSvc = new TextView(this);
                tvSvc.setText(service);
                tvSvc.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                tvSvc.setTextSize(14f);

                row.addView(check);
                row.addView(tvSvc);
                containerServices.addView(row);
            }
        }

        if (btnSelectPackage != null) {
            btnSelectPackage.setOnClickListener(v -> {
                Intent intent = new Intent(PackageDetailActivity.this, NewBookingCustomerActivity.class);
                intent.putExtra("SELECTED_PACKAGE", servicePackage.getName());
                intent.putExtra("SELECTED_PRICE", servicePackage.getStartingPrice());
                startActivity(intent);
            });
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
