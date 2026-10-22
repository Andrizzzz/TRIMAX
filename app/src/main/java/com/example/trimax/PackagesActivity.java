package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


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
                // Keep the content below the status bar, but don't shrink the whole layout
                // by applying the bottom navigation-bar inset to the root.
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
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

        View cardBottomNavContainer = findViewById(R.id.cardBottomNavContainer);
        if (cardBottomNavContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(cardBottomNavContainer, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                int baseMargin = Math.round(16 * getResources().getDisplayMetrics().density);
                params.bottomMargin = baseMargin + systemBars.bottom;
                v.setLayoutParams(params);
                return insets;
            });
        }

        setupBottomNavigation();
    }

    private List<ServicePackage> getServicePackages() {
        List<ServicePackage> list = new ArrayList<>();
        list.add(new ServicePackage(
                "pkg_coke",
                "Coke Case (24 Bottles)",
                "Wholesale Coca-Cola beverage case.",
                180.00,
                R.drawable.ic_delivery_van,
                Arrays.asList("24 bottles per case", "Original Coca-Cola taste", "Wholesale price"),
                "15 kg",
                "40 x 30 x 30 cm",
                "In stock"
        ));
        list.add(new ServicePackage(
                "pkg_sprite",
                "Sprite Case (24 Bottles)",
                "Wholesale Sprite lemon-lime beverage case.",
                180.00,
                R.drawable.ic_delivery_van,
                Arrays.asList("24 bottles per case", "Refreshing lemon-lime", "Wholesale price"),
                "15 kg",
                "40 x 30 x 30 cm",
                "In stock"
        ));
        list.add(new ServicePackage(
                "pkg_royal",
                "Royal Case (24 Bottles)",
                "Wholesale Royal orange soda beverage case.",
                175.00,
                R.drawable.ic_delivery_van,
                Arrays.asList("24 bottles per case", "Sweet orange flavor", "Wholesale price"),
                "15 kg",
                "40 x 30 x 30 cm",
                "In stock"
        ));
        list.add(new ServicePackage(
                "pkg_standard",
                "Standard Item Parcel",
                "General merchandise or standard box item.",
                99.00,
                R.drawable.ic_delivery_scooter,
                Arrays.asList("General merchandise", "Standard handling", "Secure transit"),
                "Up to 5 kg",
                "50 x 40 x 30 cm",
                "Available"
        ));
        list.add(new ServicePackage(
                "pkg_fragile",
                "Fragile Item Box",
                "Delicate items requiring careful handling.",
                499.00,
                R.drawable.ic_delivery_scooter,
                Arrays.asList("Extra bubble wrap", "Fragile handling", "Priority delivery"),
                "Up to 10 kg",
                "50 x 40 x 30 cm",
                "Available"
        ));
        return list;
    }

    private void setupBottomNavigation() {
        highlightBottomNavItem(R.id.nav_packages);

        View home = findViewById(R.id.nav_home);
        View bookings = findViewById(R.id.nav_bookings);
        View packages = findViewById(R.id.nav_packages);
        View clients = findViewById(R.id.nav_clients);
        View profile = findViewById(R.id.nav_profile);

        if (home != null) home.setOnClickListener(v -> {
            highlightBottomNavItem(R.id.nav_home);
            Intent intent = new Intent(PackagesActivity.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        if (bookings != null) bookings.setOnClickListener(v -> {
            highlightBottomNavItem(R.id.nav_bookings);
            startActivity(new Intent(this, BookingsActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        if (packages != null) packages.setOnClickListener(v -> highlightBottomNavItem(R.id.nav_packages));
        if (clients != null) clients.setOnClickListener(v -> {
            highlightBottomNavItem(R.id.nav_clients);
            startActivity(new Intent(this, ClientsActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        if (profile != null) profile.setOnClickListener(v -> {
            highlightBottomNavItem(R.id.nav_profile);
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void highlightBottomNavItem(int activeId) {
        int[] ids = {R.id.nav_home, R.id.nav_bookings, R.id.nav_packages, R.id.nav_clients, R.id.nav_profile};
        int activeIndex = 0;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == activeId) {
                activeIndex = i;
                break;
            }
        }

        for (int id : ids) {
            View v = findViewById(id);
            if (v != null) {
                boolean isActive = (id == activeId);
                v.setSelected(isActive);
            }
        }

        View indicator = findViewById(R.id.slidingIndicator);
        LinearLayout navContainer = findViewById(R.id.bottomNavigation);
        if (indicator != null && navContainer != null) {
            final int index = activeIndex;
            navContainer.post(() -> {
                int totalWidth = navContainer.getWidth() - navContainer.getPaddingLeft() - navContainer.getPaddingRight();
                if (totalWidth <= 0) return;
                int tabWidth = totalWidth / 5;

                android.view.ViewGroup.LayoutParams lp = indicator.getLayoutParams();
                int marginHorizontal = (int) (4 * getResources().getDisplayMetrics().density);
                lp.width = tabWidth - (marginHorizontal * 2);
                indicator.setLayoutParams(lp);

                float targetX = navContainer.getPaddingLeft() + (index * tabWidth) + marginHorizontal;
                indicator.animate()
                        .translationX(targetX)
                        .setDuration(220)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
            });
        }
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
            holder.tvPrice.setText(String.format(Locale.US, "₱%.2f", pkg.getStartingPrice()));
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
