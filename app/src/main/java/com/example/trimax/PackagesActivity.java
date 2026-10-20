package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PackagesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_packages);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        RecyclerView recyclerView = findViewById(R.id.recyclerViewPackages);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            List<ServicePackage> packages = getServicePackages();
            PackagesAdapter adapter = new PackagesAdapter(packages, pkg -> {
                Intent intent = new Intent(PackagesActivity.this, PackageDetailActivity.class);
                intent.putExtra("SERVICE_PACKAGE", pkg);
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_packages);
            setupBottomNavigation(bottomNavigation);
        }
    }

    private List<ServicePackage> getServicePackages() {
        List<ServicePackage> list = new ArrayList<>();
        list.add(new ServicePackage(
                "pkg_basic",
                "Basic Delivery",
                "For small parcels and documents.",
                99.00,
                R.drawable.ic_delivery_scooter,
                Arrays.asList("Safe and secure delivery", "Real-time tracking", "Delivery confirmation"),
                "Up to 3 kg",
                "30 x 20 x 15 cm",
                "1 day (same-day / next-day)"
        ));
        list.add(new ServicePackage(
                "pkg_express",
                "Express Delivery",
                "For urgent deliveries (available in select areas).",
                249.00,
                R.drawable.ic_delivery_van,
                Arrays.asList("Priority dispatch", "Safe and secure delivery", "Real-time tracking", "Delivery confirmation"),
                "Up to 5 kg",
                "40 x 30 x 20 cm",
                "2 - 4 hours"
        ));
        list.add(new ServicePackage(
                "pkg_bulk",
                "Bulk Delivery",
                "For multiple parcels or large items.",
                399.00,
                R.drawable.ic_delivery_van,
                Arrays.asList("Multi-item handling", "Secure cargo loading", "Real-time tracking", "Delivery confirmation"),
                "Up to 25 kg",
                "80 x 60 x 50 cm",
                "1 - 2 days"
        ));
        list.add(new ServicePackage(
                "pkg_fragile",
                "Fragile Delivery",
                "For fragile items needing special care.",
                299.00,
                R.drawable.ic_delivery_scooter,
                Arrays.asList("Extra padding & care", "Hand-to-hand transfer", "Real-time tracking", "Delivery confirmation"),
                "Up to 10 kg",
                "50 x 40 x 30 cm",
                "1 day"
        ));
        return list;
    }

    private void setupBottomNavigation(BottomNavigationView bottomNavigation) {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                startActivity(new Intent(this, BookingsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_packages) {
                return true;
            } else if (itemId == R.id.nav_clients) {
                startActivity(new Intent(this, ClientsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    // Adapter for Service Packages Grid
    private static class PackagesAdapter extends RecyclerView.Adapter<PackagesAdapter.ViewHolder> {
        private final List<ServicePackage> packages;
        private final OnPackageClickListener listener;

        interface OnPackageClickListener {
            void onPackageClick(ServicePackage pkg);
        }

        public PackagesAdapter(List<ServicePackage> packages, OnPackageClickListener listener) {
            this.packages = packages;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service_package, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ServicePackage pkg = packages.get(position);
            holder.tvName.setText(pkg.getName());
            holder.tvDesc.setText(pkg.getDescription());
            holder.tvPrice.setText(String.format(Locale.US, "from ₱%.0f", pkg.getStartingPrice()));
            holder.ivImage.setImageResource(pkg.getImageResId());

            holder.itemView.setOnClickListener(v -> listener.onPackageClick(pkg));
            holder.btnAdd.setOnClickListener(v -> listener.onPackageClick(pkg));
        }

        @Override
        public int getItemCount() {
            return packages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvPrice;
            ImageView ivImage, btnAdd;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvPackageName);
                tvDesc = itemView.findViewById(R.id.tvPackageDescription);
                tvPrice = itemView.findViewById(R.id.tvPackagePrice);
                ivImage = itemView.findViewById(R.id.ivPackageImage);
                btnAdd = itemView.findViewById(R.id.btnAddPackage);
            }
        }
    }
}
