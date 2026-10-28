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
import java.util.List;
import java.util.Locale;

public class PackagesActivity extends AppCompatActivity {

    private final List<ServicePackage> items = new ArrayList<>();
    private PackagesAdapter adapter;
    private TextView tvState;

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
            adapter = new PackagesAdapter(items, pkg -> {
                Intent intent = new Intent(PackagesActivity.this, PackageDetailActivity.class);
                intent.putExtra("SERVICE_PACKAGE", pkg);
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        }
        tvState = findViewById(R.id.tvPackagesState);
        loadProducts();

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

    /**
     * The agent's item price list: the products, prices, stock and pictures the admin keeps in the
     * Desktop inventory (public.trimax_list_products), instead of the built-in sample list.
     */
    private void loadProducts() {
        showState("Loading items...", false);
        ProductImageLoader.get(this).retryFailed();
        TrimaxApi.get(this).listProducts(new TrimaxApi.Callback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> products) {
                if (SessionGuard.isGone(PackagesActivity.this)) return;
                items.clear();
                for (Product p : products) {
                    ServicePackage pkg = new ServicePackage("product:" + p.id, p.name,
                            (p.stock > 0 ? "In stock: " + Product.stockText(p.stock) : "Out of stock")
                                    + (p.sellsHalfCase() ? String.format(java.util.Locale.US, " · Half case ₱ %.2f", p.halfPrice) : ""),
                            p.price, R.drawable.ic_delivery_van, null, null, null, null);
                    pkg.setImageUrl(p.imageUrl());
                    items.add(pkg);
                }
                if (adapter != null) adapter.notifyDataSetChanged();
                if (items.isEmpty()) showState("No items yet. The admin adds them in the Desktop inventory.", false);
                else showState(null, false);
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(PackagesActivity.this)) return;
                if (SessionGuard.handle(PackagesActivity.this, error)) return;
                showState(error.getMessage() + "  Tap to try again.", true);
            }
        });
    }

    private void showState(String text, boolean tapToRetry) {
        if (tvState == null) return;
        tvState.setVisibility(text == null ? View.GONE : View.VISIBLE);
        tvState.setText(text == null ? "" : text);
        tvState.setOnClickListener(tapToRetry ? v -> loadProducts() : null);
        tvState.setClickable(tapToRetry);
    }

    private void navigateToTab(Class<?> targetClass) {
        Intent intent = new Intent(PackagesActivity.this, targetClass);
        intent.putExtra("PREV_TAB_INDEX", 2);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    private void setupBottomNavigation() {
        highlightBottomNavItem(R.id.nav_packages);

        View home = findViewById(R.id.nav_home);
        View bookings = findViewById(R.id.nav_bookings);
        View packages = findViewById(R.id.nav_packages);
        View clients = findViewById(R.id.nav_clients);
        View profile = findViewById(R.id.nav_profile);

        if (home != null) home.setOnClickListener(v -> navigateToTab(DashboardActivity.class));
        if (bookings != null) bookings.setOnClickListener(v -> navigateToTab(BookingsActivity.class));
        if (packages != null) packages.setOnClickListener(v -> highlightBottomNavItem(R.id.nav_packages));
        if (clients != null) clients.setOnClickListener(v -> navigateToTab(ClientsActivity.class));
        if (profile != null) profile.setOnClickListener(v -> navigateToTab(ProfileActivity.class));
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

        int prevIndex = getIntent().getIntExtra("PREV_TAB_INDEX", -1);
        getIntent().removeExtra("PREV_TAB_INDEX");

        View indicator = findViewById(R.id.slidingIndicator);
        LinearLayout navContainer = findViewById(R.id.bottomNavigation);
        if (indicator != null) {
            final int targetIndex = activeIndex;
            final int initialIndex = (prevIndex >= 0 && prevIndex != targetIndex) ? prevIndex : targetIndex;

            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int totalWidth = screenWidth - (int) (52 * getResources().getDisplayMetrics().density);
            final int[] effectiveTabWidth = {totalWidth / 5};
            int marginHorizontal = (int) (4 * getResources().getDisplayMetrics().density);

            ViewGroup.LayoutParams lp = indicator.getLayoutParams();
            if (lp != null) {
                lp.width = effectiveTabWidth[0] - (marginHorizontal * 2);
                indicator.setLayoutParams(lp);
            }
            float startX = (navContainer != null ? navContainer.getPaddingLeft() : (int)(20 * getResources().getDisplayMetrics().density)) + (initialIndex * effectiveTabWidth[0]) + marginHorizontal;
            indicator.setTranslationX(startX);

            if (navContainer != null && prevIndex >= 0 && prevIndex != targetIndex) {
                navContainer.post(() -> {
                    int measuredWidth = navContainer.getWidth() - navContainer.getPaddingLeft() - navContainer.getPaddingRight();
                    if (measuredWidth > 0) {
                        effectiveTabWidth[0] = measuredWidth / 5;
                        if (lp != null) {
                            lp.width = effectiveTabWidth[0] - (marginHorizontal * 2);
                            indicator.setLayoutParams(lp);
                        }
                    }

                    float targetX = navContainer.getPaddingLeft() + (targetIndex * effectiveTabWidth[0]) + marginHorizontal;

                    indicator.animate()
                            .translationX(targetX)
                            .setDuration(380)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                            .start();
                });
            }
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
            ProductImageLoader.get(holder.itemView.getContext()).load(holder.ivImage, pkg.getImageUrl(),
                    pkg.getImageResId(), Math.round(180 * holder.itemView.getResources().getDisplayMetrics().density));

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
