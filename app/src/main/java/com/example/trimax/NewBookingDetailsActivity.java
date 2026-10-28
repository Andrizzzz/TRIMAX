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
import com.google.android.material.chip.Chip;
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
    /**
     * Products as the server listed them, by card (for the stock, and for "Less": allowed or not, and the most
     * per piece). A product with a half case price has a second card, "<name> (half case)" (id ending in
     * HALF_SUFFIX), at the half case price; both cards share the product's stock (a half case takes 0.5).
     */
    private final java.util.Map<String, Product> productByPackage = new java.util.HashMap<>();
    private static final String HALF_SUFFIX = ":HALF";
    /** "Less" per piece the agent picked on an item card (only items the Admin allows). */
    private final java.util.Map<String, java.math.BigDecimal> lessByPackage = new java.util.HashMap<>();

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

        pickupLat = BookingRecord.HUB_LATITUDE;
        pickupLng = BookingRecord.HUB_LONGITUDE;
        destLat = intent.getDoubleExtra("DEST_LAT", 0);
        destLng = intent.getDoubleExtra("DEST_LNG", 0);

        ImageView btnBack = findViewById(R.id.btnBack);
        containerPackageCards = findViewById(R.id.containerPackageCards);
        etNotes = findViewById(R.id.etNotes);
        btnContinue = findViewById(R.id.btnContinue);
        btnBackStep = findViewById(R.id.btnBackStep);

        servicePackages = new ArrayList<>();
        loadProducts();

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
                double totalLess = 0;
                int totalQuantity = 0;
                List<ServicePackage> chosen = new ArrayList<>();

                for (ServicePackage pkg : servicePackages) {
                    if (pkg.getQuantity() > 0) {
                        if (serviceBuilder.length() > 0) {
                            serviceBuilder.append(", ");
                        }
                        serviceBuilder.append(pkg.getName()).append(" (x").append(pkg.getQuantity()).append(")");
                        totalPrice += pkg.getStartingPrice() * pkg.getQuantity();
                        totalLess += lessOf(pkg).doubleValue() * pkg.getQuantity();
                        totalQuantity += pkg.getQuantity();
                        chosen.add(pkg);
                    }
                }
                // Products and quantities travel to the summary; the server prices them again when saving.
                int[] productIds = new int[chosen.size()];
                int[] productQuantities = new int[chosen.size()];
                double[] productLess = new double[chosen.size()];
                for (int i = 0; i < chosen.size(); i++) {
                    productIds[i] = productIdOf(chosen.get(i));
                    productQuantities[i] = chosen.get(i).getQuantity();
                    productLess[i] = lessOf(chosen.get(i)).doubleValue();
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
                summaryIntent.putExtra("PRODUCT_IDS", productIds);
                summaryIntent.putExtra("PRODUCT_QUANTITIES", productQuantities);
                summaryIntent.putExtra("ESTIMATED_TOTAL", totalPrice);           // before any discount
                summaryIntent.putExtra("PRODUCT_DISCOUNTS", productLess);       // less per piece, same order
                summaryIntent.putExtra("ESTIMATED_DISCOUNT", totalLess);
                startActivity(summaryIntent);
            });
        }
    }

    // The products, prices and stock are the ones on the TRIMAX server (the desktop Inventory), not a list
    // built into the app, so what the agent orders is what the warehouse actually has.
    private void loadProducts() {
        if (btnContinue != null) btnContinue.setEnabled(false);
        ProductImageLoader.get(this).retryFailed();
        showProductMessage("Loading products...", false);
        TrimaxApi.get(this).listProducts(new TrimaxApi.Callback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> products) {
                if (SessionGuard.isGone(NewBookingDetailsActivity.this)) return;
                servicePackages = new ArrayList<>();
                productByPackage.clear();
                for (Product p : products) {
                    String stockText = p.stock > 0 ? "In stock: " + Product.stockText(p.stock) : "Out of stock";
                    ServicePackage pkg = new ServicePackage("product:" + p.id, p.name, stockText,
                            p.price, R.drawable.ic_delivery_van, null, "", "", "");
                    pkg.setImageUrl(p.imageUrl());   // picture set by the admin on the Desktop, if any
                    servicePackages.add(pkg);
                    productByPackage.put(pkg.getId(), p);
                    if (p.sellsHalfCase()) {
                        // Half case (database update 20261028_07): its own card at the half case price.
                        ServicePackage half = new ServicePackage("product:" + p.id + HALF_SUFFIX, p.name + " (half case)",
                                "Half case · " + stockText, p.halfPrice, R.drawable.ic_delivery_van, null, "", "", "");
                        half.setImageUrl(p.imageUrl());
                        servicePackages.add(half);
                        productByPackage.put(half.getId(), p);
                    }
                }
                // A "Less" picked before a refresh stays only if the item still allows it.
                for (java.util.Iterator<java.util.Map.Entry<String, java.math.BigDecimal>> it = lessByPackage.entrySet().iterator(); it.hasNext(); ) {
                    java.util.Map.Entry<String, java.math.BigDecimal> e = it.next();
                    Product p = productByPackage.get(e.getKey());
                    if (p == null || ItemDiscounts.problem(p.discountLine(1, e.getKey().endsWith(HALF_SUFFIX)), e.getValue()) != null) it.remove();
                }
                if (servicePackages.isEmpty()) {
                    showProductMessage("No products are available. Ask your administrator.", true);
                    return;
                }
                // Started from an item in the price list ("Book this item"): put one of it in the order.
                String chosen = getIntent().getStringExtra("SELECTED_PACKAGE");
                if (chosen != null) {
                    for (ServicePackage pkg : servicePackages) {
                        if (chosen.equalsIgnoreCase(pkg.getName()) && stockProblem(pkg) == null && pkg.getQuantity() == 0) {
                            pkg.setQuantity(1);
                            break;
                        }
                    }
                }
                populatePackageCards();
                if (btnContinue != null) btnContinue.setEnabled(true);
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(NewBookingDetailsActivity.this)) return;
                if (SessionGuard.handle(NewBookingDetailsActivity.this, error)) return;
                showProductMessage(error.getMessage(), true);
            }
        });
    }

    private void showProductMessage(String message, boolean withRetry) {
        if (containerPackageCards == null) return;
        containerPackageCards.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(15);
        tv.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad / 2);
        containerPackageCards.addView(tv);
        if (withRetry) {
            MaterialButton retry = new MaterialButton(this);
            retry.setText("Retry");
            retry.setOnClickListener(v -> loadProducts());
            containerPackageCards.addView(retry);
        }
    }

    /** The card's line key: the product id, or minus it for the half case card (see ItemDiscounts.key). */
    private static int productIdOf(ServicePackage pkg) {
        try {
            String id = pkg.getId().substring("product:".length());
            boolean half = id.endsWith(HALF_SUFFIX);
            if (half) id = id.substring(0, id.length() - HALF_SUFFIX.length());
            return ItemDiscounts.key(Integer.parseInt(id), half);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static boolean isHalf(ServicePackage pkg) {
        return pkg.getId() != null && pkg.getId().endsWith(HALF_SUFFIX);
    }

    /** Cases of this product already in the order (whole cards count 1, half case cards 0.5 each). */
    private double casesInOrder(Product product) {
        double n = 0;
        for (ServicePackage other : servicePackages) {
            if (other.getQuantity() > 0 && productByPackage.get(other.getId()) == product) {
                n += other.getQuantity() * (isHalf(other) ? 0.5 : 1);
            }
        }
        return n;
    }

    /** Why one more of this card does not fit the product's stock, or null when it does. */
    private String stockProblem(ServicePackage pkg) {
        Product product = productByPackage.get(pkg.getId());
        if (product == null) return null;
        if (product.stock <= 0) return "Out of stock.";
        double after = casesInOrder(product) + (isHalf(pkg) ? 0.5 : 1);
        if (after > product.stock + 1e-9) {
            return "Only " + Product.stockText(product.stock) + " in stock"
                    + (isHalf(pkg) ? " (a half case takes 0.5)." : ".");
        }
        return null;
    }

    private java.math.BigDecimal lessOf(ServicePackage pkg) {
        java.math.BigDecimal v = lessByPackage.get(pkg.getId());
        return v == null ? ItemDiscounts.scale(null) : v;
    }

    /** Price line of a card: "₱ 25.00", or with a discount "<s>₱ 25.00</s>  ₱ 23.00 each" and a green Less button. */
    private void showPrice(ServicePackage pkg, TextView tvPrice, TextView tvNet, Chip chipLess) {
        Product product = productByPackage.get(pkg.getId());
        java.math.BigDecimal less = lessOf(pkg);
        boolean on = less.signum() > 0;
        if (tvPrice != null) {
            tvPrice.setText(String.format(Locale.US, "₱ %.2f", pkg.getStartingPrice()));
            ItemLessSheet.strike(tvPrice, on);
            tvPrice.setTextColor(androidx.core.content.ContextCompat.getColor(this, on ? R.color.text_secondary : R.color.primary_blue));
            tvPrice.setTypeface(null, on ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
        }
        if (tvNet != null) {
            tvNet.setVisibility(on ? View.VISIBLE : View.GONE);
            if (on) {
                tvNet.setText(String.format(Locale.US, "₱ %.2f each",
                        ItemDiscounts.net(java.math.BigDecimal.valueOf(pkg.getStartingPrice()), less)));
            }
        }
        if (chipLess == null) return;
        boolean allowed = product != null && (isHalf(pkg) ? product.halfDiscountAllowed() : product.discountAllowed);
        if (!allowed) {
            chipLess.setVisibility(View.GONE);
            return;
        }
        chipLess.setVisibility(View.VISIBLE);
        chipLess.setText(ItemDiscounts.buttonText(less));
        int fg = androidx.core.content.ContextCompat.getColor(this, on ? R.color.status_confirmed : R.color.primary_blue);
        int bg = androidx.core.content.ContextCompat.getColor(this, on ? R.color.status_confirmed_bg : R.color.surface_alt);
        chipLess.setTextColor(fg);
        chipLess.setChipIconTint(android.content.res.ColorStateList.valueOf(fg));
        chipLess.setCloseIconTint(android.content.res.ColorStateList.valueOf(fg));
        chipLess.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(bg));
        chipLess.setContentDescription(on ? "Less " + DiscountCalc.peso(less) + " per piece. Tap to change."
                : "Take something off each piece of " + pkg.getName());
    }

    private void openLess(ServicePackage pkg, TextView tvPrice, TextView tvNet, Chip chipLess) {
        Product product = productByPackage.get(pkg.getId());
        boolean half = isHalf(pkg);
        if (product == null || !(half ? product.halfDiscountAllowed() : product.discountAllowed)) return;
        ItemLessSheet.show(this, product.discountLine(Math.max(1, pkg.getQuantity()), half), lessOf(pkg), perUnit -> {
            if (perUnit.signum() > 0) lessByPackage.put(pkg.getId(), perUnit);
            else lessByPackage.remove(pkg.getId());
            showPrice(pkg, tvPrice, tvNet, chipLess);
        });
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
            TextView tvNet = cardView.findViewById(R.id.tvPackageNetPrice);
            Chip chipLess = cardView.findViewById(R.id.chipLess);
            TextView tvItemQuantity = cardView.findViewById(R.id.tvItemQuantity);
            MaterialButton btnMinus = cardView.findViewById(R.id.btnMinus);
            MaterialButton btnPlus = cardView.findViewById(R.id.btnPlus);

            if (ivImage != null) {
                ProductImageLoader.get(this).load(ivImage, pkg.getImageUrl(), pkg.getImageResId(),
                        Math.round(56 * getResources().getDisplayMetrics().density));
            }
            if (tvName != null) tvName.setText(pkg.getName());
            if (tvDesc != null) tvDesc.setText(pkg.getDescription());
            showPrice(pkg, tvPrice, tvNet, chipLess);
            if (chipLess != null) {
                chipLess.setOnClickListener(v -> openLess(pkg, tvPrice, tvNet, chipLess));
                chipLess.setOnCloseIconClickListener(v -> openLess(pkg, tvPrice, tvNet, chipLess));
            }
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
                    // Whole and half case cards of a product share its stock (a half case takes 0.5).
                    String problem = stockProblem(pkg);
                    if (problem != null) {
                        android.widget.Toast.makeText(this, problem, android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
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
            btnContinue.setEnabled(servicePackages != null && !servicePackages.isEmpty());
        }
    }
}
