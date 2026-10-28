package com.example.trimax;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class BookingDetailActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;
    private String currentBookingId;
    private Delivery currentDelivery;

    // View References
    private TextView tvHeaderTitle;
    private TextView tvStatusBadge;
    private TextView tvService;
    private TextView tvTotalAmount;
    private TextView tvCustomerName;
    private TextView tvPhone;
    private TextView tvPickupAddress;
    private TextView tvLocationAddress;
    private TextView tvParcelDetails;
    private TextView tvSpecialInstructions;
    private View dividerNotes;
    private LinearLayout llNotesSection;
    private TextView tvBookingIdValue;
    private TextView tvScheduleTime;
    private TextView tvAssignedDriverValue;

    private MaterialButton btnViewLocation;
    private MaterialButton btnConfirmBooking;
    private MaterialButton btnCancelBooking;

    private LinearLayout llMainContent;
    private LinearLayout llErrorState;
    private TextView tvErrorTitle;
    private TextView tvErrorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking_detail);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);

        Intent intent = getIntent();
        currentBookingId = intent.getStringExtra("BOOKING_ID");
        if (TextUtils.isEmpty(currentBookingId)) {
            currentBookingId = intent.getStringExtra("DELIVERY_ID");
        }
        if (currentBookingId != null) {
            currentBookingId = currentBookingId.trim();
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvService = findViewById(R.id.tvService);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvPhone = findViewById(R.id.tvPhone);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvLocationAddress = findViewById(R.id.tvLocationAddress);
        tvParcelDetails = findViewById(R.id.tvParcelDetails);
        tvSpecialInstructions = findViewById(R.id.tvSpecialInstructions);
        dividerNotes = findViewById(R.id.dividerNotes);
        llNotesSection = findViewById(R.id.llNotesSection);
        tvBookingIdValue = findViewById(R.id.tvBookingIdValue);
        tvScheduleTime = findViewById(R.id.tvScheduleTime);
        tvAssignedDriverValue = findViewById(R.id.tvAssignedDriverValue);

        btnViewLocation = findViewById(R.id.btnViewLocation);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);

        llMainContent = findViewById(R.id.llMainContent);
        llErrorState = findViewById(R.id.llErrorState);
        tvErrorTitle = findViewById(R.id.tvErrorTitle);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        MaterialButton btnErrorBack = findViewById(R.id.btnErrorBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBackToDashboard());
        }

        if (btnErrorBack != null) {
            btnErrorBack.setOnClickListener(v -> navigateBackToDashboard());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBackToDashboard();
            }
        });

        // The booking is loaded in onResume, which always follows onCreate.

        // Phone Dial Action
        if (tvPhone != null) {
            tvPhone.setOnClickListener(v -> {
                String phoneStr = tvPhone.getText() != null ? tvPhone.getText().toString().replaceAll("[^0-9+]", "") : "";
                if (!TextUtils.isEmpty(phoneStr)) {
                    try {
                        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneStr));
                        startActivity(dialIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(BookingDetailActivity.this, "Dialer app not available on this device", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(BookingDetailActivity.this, "No phone number available for this customer", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // View Location on Map Action
        if (btnViewLocation != null) {
            btnViewLocation.setOnClickListener(v -> {
                if (currentDelivery == null) return;
                Intent locIntent = new Intent(BookingDetailActivity.this, TrackingMapActivity.class);
                locIntent.putExtra("BOOKING_ID", currentDelivery.getBookingId());
                locIntent.putExtra("DELIVERY_ID", currentDelivery.getBookingId());
                locIntent.putExtra("CUSTOMER_NAME", currentDelivery.getCustomerName());
                locIntent.putExtra("DESTINATION", currentDelivery.getDestination());
                locIntent.putExtra("DEST_LAT", currentDelivery.getDestinationLatitude());
                locIntent.putExtra("DEST_LNG", currentDelivery.getDestinationLongitude());
                try {
                    startActivity(locIntent);
                } catch (Exception e) {
                    Toast.makeText(BookingDetailActivity.this, "Unable to open map view", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Confirming and cancelling happen on the desktop (an administrator decides, and cancelling returns the
        // stock). The buttons stay hidden; if a layout ever shows them, they explain instead of changing data.
        View.OnClickListener adminOnly = v -> Toast.makeText(this,
                "Bookings are confirmed or cancelled by the administrator in the TRIMAX desktop app.",
                Toast.LENGTH_LONG).show();
        if (btnConfirmBooking != null) btnConfirmBooking.setOnClickListener(adminOnly);
        if (btnCancelBooking != null) btnCancelBooking.setOnClickListener(adminOnly);

        View btnEditDiscount = findViewById(R.id.btnEditDiscount);
        if (btnEditDiscount != null) btnEditDiscount.setOnClickListener(v -> showDiscountDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookingDetails();
    }

    /** The booking as the TRIMAX server has it (the agent's own bookings only). */
    private BookingRecord currentRecord;

    private void loadBookingDetails() {
        int id = 0;
        try {
            id = Integer.parseInt(currentBookingId != null ? currentBookingId.replace("#", "").trim() : "");
        } catch (NumberFormatException ignored) {
            // Older local bookings used codes like BK-2026-00125; they were never sent to the server.
        }
        if (id <= 0) {
            showErrorState("Booking Not Found", "Booking " + (currentBookingId != null ? currentBookingId : "")
                    + " was only saved on this phone by an older version of the app and is not on the TRIMAX server.");
            return;
        }
        if (currentDelivery == null && tvHeaderTitle != null) tvHeaderTitle.setText("Loading booking...");
        TrimaxApi.get(this).getMyBooking(id, new TrimaxApi.Callback<BookingRecord>() {
            @Override
            public void onSuccess(BookingRecord record) {
                if (SessionGuard.isGone(BookingDetailActivity.this)) return;
                currentRecord = record;
                currentDelivery = record.toDelivery(false);
                bindBooking();
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(BookingDetailActivity.this)) return;
                if (SessionGuard.handle(BookingDetailActivity.this, error)) return;
                if (currentDelivery == null) {
                    showErrorState(error.getKind() == ApiException.Kind.NOT_FOUND ? "Booking Not Found" : "Could Not Load Booking",
                            error.getMessage());
                } else {
                    Toast.makeText(BookingDetailActivity.this, "Could not refresh: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void bindBooking() {
        hideErrorState();

        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("Booking #" + currentDelivery.getBookingId());
        }

        if (tvBookingIdValue != null) {
            tvBookingIdValue.setText(currentDelivery.getBookingId());
        }

        if (tvCustomerName != null) {
            tvCustomerName.setText(TextUtils.isEmpty(currentDelivery.getCustomerName()) ? "Customer" : currentDelivery.getCustomerName());
        }

        if (tvPhone != null) {
            String phone = currentDelivery.getCustomerPhone();
            tvPhone.setText(TextUtils.isEmpty(phone) ? "No phone provided" : "📞 " + phone);
        }

        if (tvService != null) {
            String details = currentDelivery.getPackageDetails();
            tvService.setText(TextUtils.isEmpty(details) ? "Package details unavailable" : details);
        }

        if (tvPickupAddress != null) {
            String pickup = currentDelivery.getPickupLocation();
            tvPickupAddress.setText(TextUtils.isEmpty(pickup) ? "Not specified" : pickup);
        }

        if (tvLocationAddress != null) {
            String dest = currentDelivery.getDestination();
            tvLocationAddress.setText(TextUtils.isEmpty(dest) ? "Not specified" : dest);
        }

        if (tvParcelDetails != null) {
            String details = currentDelivery.getPackageDetails();
            tvParcelDetails.setText(TextUtils.isEmpty(details) ? "No parcel details provided" : details);
        }

        if (tvScheduleTime != null) {
            String sched = currentDelivery.getScheduledTime();
            if (TextUtils.isEmpty(sched)) {
                sched = currentDelivery.getCreatedAt();
            }
            tvScheduleTime.setText(TextUtils.isEmpty(sched) ? "Not scheduled" : sched);
        }

        if (tvSpecialInstructions != null) {
            String notes = currentDelivery.getSpecialInstructions();
            if (!TextUtils.isEmpty(notes) && !notes.trim().equalsIgnoreCase("none")) {
                tvSpecialInstructions.setText(notes);
                if (llNotesSection != null) llNotesSection.setVisibility(View.VISIBLE);
                if (dividerNotes != null) dividerNotes.setVisibility(View.VISIBLE);
            } else {
                tvSpecialInstructions.setText("None");
                if (llNotesSection != null) llNotesSection.setVisibility(View.GONE);
                if (dividerNotes != null) dividerNotes.setVisibility(View.GONE);
            }
        }

        if (tvAssignedDriverValue != null) {
            String driver = currentDelivery.getDriverId();
            if (TextUtils.isEmpty(driver) || "Unassigned".equalsIgnoreCase(driver)) {
                tvAssignedDriverValue.setText("Not yet assigned");
                tvAssignedDriverValue.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
            } else {
                if (currentRecord != null) {
                    String progress = currentRecord.deliveryStatus == null ? ""
                            : " \u2022 " + DeliveryProgress.driverDisplayStatus(currentRecord.status, currentRecord.deliveryStatus);
                    driver = driver + (currentRecord.vehicleId != null ? " \u2022 " + currentRecord.vehicleId : "") + progress;
                }
                tvAssignedDriverValue.setText(driver);
                tvAssignedDriverValue.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
            }
        }

        if (tvTotalAmount != null) {
            tvTotalAmount.setText(String.format(Locale.US, "₱ %,.2f", currentDelivery.getCurrentTotal()));
        }
        // Original price, the agent's discount and the final price (the total above).
        View breakdown = findViewById(R.id.llDiscountBreakdown);
        if (breakdown != null) {
            boolean discounted = currentRecord != null && currentRecord.hasDiscount();
            breakdown.setVisibility(discounted ? View.VISIBLE : View.GONE);
            if (discounted) {
                TextView original = findViewById(R.id.tvOriginalAmount);
                TextView label = findViewById(R.id.tvDiscountGiven);
                TextView amount = findViewById(R.id.tvDiscountGivenAmount);
                if (original != null) original.setText(String.format(Locale.US, "₱ %,.2f", currentRecord.subtotalAmount));
                if (amount != null) amount.setText(String.format(Locale.US, "- ₱ %,.2f", currentRecord.discountAmount));
                if (label != null) {
                    String text = "PERCENT".equals(currentRecord.discountType) && currentRecord.discountValue != null
                            ? "Discount (" + java.math.BigDecimal.valueOf(currentRecord.discountValue).stripTrailingZeros().toPlainString() + "%)"
                            : "PER_ITEM".equals(currentRecord.discountType) ? "Discount on items" : "Discount";
                    if (currentRecord.discountBy != null) text += " by " + currentRecord.discountBy;
                    label.setText(text);
                }
            }
        }

        updateStatusUi(currentDelivery.getStatus());

        // The agent may give or change a discount while the booking is still Pending.
        com.google.android.material.button.MaterialButton btnEditDiscount = findViewById(R.id.btnEditDiscount);
        if (btnEditDiscount != null) {
            boolean pending = currentRecord != null && "Pending".equalsIgnoreCase(currentRecord.status)
                    && !currentRecord.items.isEmpty();
            btnEditDiscount.setVisibility(pending ? View.VISIBLE : View.GONE);
            btnEditDiscount.setText(currentRecord != null && currentRecord.hasDiscount() ? "Change discount" : "Give a discount");
        }
    }

    /**
     * Discount per item on this Pending booking (only items the Admin allows, never below puhunan + minimum
     * patong). Empty boxes = full price; the server checks every line again.
     */
    private void showDiscountDialog() {
        if (currentRecord == null) return;
        final BookingRecord record = currentRecord;
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        android.widget.LinearLayout rowsBox = new android.widget.LinearLayout(this);
        rowsBox.setOrientation(android.widget.LinearLayout.VERTICAL);
        TextView tvLimit = new TextView(this);
        tvLimit.setText("Checking which items can be discounted...");
        tvLimit.setTextSize(12);
        TextView tvPreview = new TextView(this);
        tvPreview.setPadding(0, pad / 2, 0, 0);
        box.addView(rowsBox);
        box.addView(tvLimit);
        box.addView(tvPreview);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(box);

        final java.util.Map<Integer, java.math.BigDecimal> current = new java.util.HashMap<>();
        for (BookingRecord.Item it : record.items) {
            if (it.discountPerUnit > 0) current.put(ItemDiscounts.key(it.productId, it.half), java.math.BigDecimal.valueOf(it.discountPerUnit));
        }
        final DiscountQuote[] quote = new DiscountQuote[1];
        final ItemDiscountRows[] rows = new ItemDiscountRows[1];
        Runnable refresh = () -> {
            if (quote[0] == null || quote[0].items.isEmpty()) return;
            ItemDiscounts.Totals t = ItemDiscounts.totals(quote[0].items, rows[0].values());
            tvPreview.setText("Original " + DiscountCalc.peso(t.subtotal) + "   Discount " + DiscountCalc.peso(t.discount)
                    + "\nFinal " + DiscountCalc.peso(t.total()));
        };
        rows[0] = new ItemDiscountRows(this, rowsBox, refresh);

        java.util.List<CreateBookingRequest.Line> lines = new java.util.ArrayList<>();
        for (BookingRecord.Item it : record.items) lines.add(new CreateBookingRequest.Line(it.productId, it.quantity, it.half));
        TrimaxApi.get(this).discountQuote(lines, new TrimaxApi.Callback<DiscountQuote>() {
            @Override
            public void onSuccess(DiscountQuote result) {
                if (SessionGuard.isGone(BookingDetailActivity.this)) return;
                quote[0] = result;
                rows[0].show(result.items, current);
                tvLimit.setText(result.items.isEmpty() ? "Discounts are not available on this server yet."
                        : ItemDiscounts.summary(result.items));
                refresh.run();
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(BookingDetailActivity.this)) return;
                tvLimit.setText("Could not check which items can be discounted (" + error.getMessage() + "). Close and try again.");
            }
        });

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(record.hasDiscount() ? "Change discount per item" : "Discount per item")
                .setView(scroll)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(b -> {
            if (quote[0] == null) {
                tvLimit.setText("Wait until the items are checked, then save.");
                return;
            }
            String problem = rows[0].firstProblem();
            if (problem != null) {
                tvLimit.setText(problem);
                return;
            }
            b.setEnabled(false);
            TrimaxApi.get(this).setItemDiscounts(record.bookingId, rows[0].values(), new TrimaxApi.Callback<BookingRecord>() {
                @Override
                public void onSuccess(BookingRecord updated) {
                    if (SessionGuard.isGone(BookingDetailActivity.this)) return;
                    dialog.dismiss();
                    currentRecord = updated;
                    currentDelivery = updated.toDelivery(false);
                    bindBooking();
                    Toast.makeText(BookingDetailActivity.this, updated.hasDiscount()
                            ? "Discount saved: " + updated.discountText() : "Discount removed.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(ApiException error) {
                    if (SessionGuard.isGone(BookingDetailActivity.this)) return;
                    b.setEnabled(true);
                    if (SessionGuard.handle(BookingDetailActivity.this, error)) {
                        dialog.dismiss();
                        return;
                    }
                    tvLimit.setText(error.getMessage());
                }
            });
        }));
        dialog.show();
    }

    private void updateStatusUi(String status) {
        if (tvStatusBadge == null) return;

        String upperStatus = status != null ? status.trim().toUpperCase(Locale.US) : "PENDING";

        // Confirming, assigning and cancelling are done by the administrator in the desktop app (cancelling
        // also returns the stock), so this screen only shows the booking.
        if (btnCancelBooking != null) btnCancelBooking.setVisibility(View.GONE);

        if ("PENDING".equals(upperStatus) || upperStatus.isEmpty()) {
            tvStatusBadge.setText("PENDING");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
            tvStatusBadge.setTextColor(0xFFD97706);
            if (btnConfirmBooking != null) btnConfirmBooking.setVisibility(View.GONE);
            return;
        }

        if ("CANCELLED".equals(upperStatus)) {
            tvStatusBadge.setText("CANCELLED");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_cancelled);
            tvStatusBadge.setTextColor(0xFFEF4444);
            if (btnConfirmBooking != null) btnConfirmBooking.setVisibility(View.GONE);
            if (btnCancelBooking != null) btnCancelBooking.setVisibility(View.GONE);
            return;
        }

        if ("COMPLETED".equals(upperStatus) || "DELIVERED".equals(upperStatus)) {
            tvStatusBadge.setText("COMPLETED");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_completed);
            tvStatusBadge.setTextColor(0xFF2563EB);
            if (btnConfirmBooking != null) btnConfirmBooking.setVisibility(View.GONE);
            if (btnCancelBooking != null) btnCancelBooking.setVisibility(View.GONE);
            return;
        }

        // Confirmed or in-progress deliveries must never be displayed as PENDING.
        tvStatusBadge.setText(upperStatus.replace('_', ' '));
        tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
        tvStatusBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_blue));
        if (btnConfirmBooking != null) btnConfirmBooking.setVisibility(View.GONE);
    }

    private void showErrorState(String title, String message) {
        if (llMainContent != null) llMainContent.setVisibility(View.GONE);
        if (llErrorState != null) llErrorState.setVisibility(View.VISIBLE);
        if (tvErrorTitle != null) tvErrorTitle.setText(title);
        if (tvErrorMessage != null) tvErrorMessage.setText(message);
        if (tvHeaderTitle != null) tvHeaderTitle.setText("Booking Error");
        if (tvStatusBadge != null) tvStatusBadge.setVisibility(View.GONE);
    }

    private void hideErrorState() {
        if (llMainContent != null) llMainContent.setVisibility(View.VISIBLE);
        if (llErrorState != null) llErrorState.setVisibility(View.GONE);
        if (tvStatusBadge != null) tvStatusBadge.setVisibility(View.VISIBLE);
    }

    private void navigateBackToDashboard() {
        // Preserve the screen that opened Booking Details (e.g. Bookings list).
        if (isTaskRoot()) {
            Intent intent = new Intent(BookingDetailActivity.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
