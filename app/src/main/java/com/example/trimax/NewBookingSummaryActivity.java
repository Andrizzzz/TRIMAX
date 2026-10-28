package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewBookingSummaryActivity extends AppCompatActivity {

    private static final String STATE_REQUEST_ID = "trimax_request_id";

    /**
     * One id per booking attempt, reused on every retry (and kept across screen rotation), so a retry after
     * a time-out returns the booking the server already saved instead of creating a second one.
     */
    private String requestId;
    private boolean submitting;

    // Discount per item: the server's quote says, per item, whether it may be discounted and the most per
    // piece (never the puhunan itself).
    private DiscountQuote quote;
    private boolean quoteLoading;
    private BigDecimal estimatedSubtotal = BigDecimal.ZERO;
    private final List<CreateBookingRequest.Line> quoteLines = new ArrayList<>();
    private ItemDiscountRows itemRows;
    /** "Less" picked on the item list (product id -> per piece); fills the boxes the first time the quote arrives. */
    private java.util.Map<Integer, BigDecimal> lessFromItems = new java.util.HashMap<>();
    private BigDecimal estimatedLess = BigDecimal.ZERO;
    private TextView tvDiscountLimit, tvSubtotalAmount, tvDiscountAmount, tvDiscountLabel, tvTotal;
    private View rowSubtotal, rowDiscount;
    private androidx.activity.result.ActivityResultLauncher<Intent> addCustomerLauncher;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_REQUEST_ID, requestId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestId = savedInstanceState != null ? savedInstanceState.getString(STATE_REQUEST_ID) : null;
        if (requestId == null) requestId = java.util.UUID.randomUUID().toString();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_booking_summary);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        Intent intent = getIntent();
        String customerName = intent.getStringExtra("CUSTOMER_NAME");
        String customerPhone = intent.getStringExtra("CUSTOMER_PHONE");
        String customerEmail = intent.getStringExtra("CUSTOMER_EMAIL");
        String service = intent.getStringExtra("SERVICE");
        int quantity = intent.getIntExtra("QUANTITY", 1);
        double selectedPrice = intent.getDoubleExtra("SELECTED_PRICE", 99.00);
        String date = intent.getStringExtra("DATE");
        String time = intent.getStringExtra("TIME");
        String resolvedAddress = intent.getStringExtra("ADDRESS");
        if (resolvedAddress == null || resolvedAddress.trim().isEmpty()) {
            resolvedAddress = intent.getStringExtra("CUSTOMER_ADDRESS");
        }
        final String address = resolvedAddress;

        double pickupLat = BookingRecord.HUB_LATITUDE;
        double pickupLng = BookingRecord.HUB_LONGITUDE;
        double destLat = intent.getDoubleExtra("DEST_LAT", 0);
        double destLng = intent.getDoubleExtra("DEST_LNG", 0);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvCustomerName = findViewById(R.id.tvCustomerName);
        TextView tvCustomerPhone = findViewById(R.id.tvCustomerPhone);
        TextView tvService = findViewById(R.id.tvService);
        TextView tvQuantity = findViewById(R.id.tvQuantity);
        TextView tvDateTime = findViewById(R.id.tvDateTime);
        TextView tvAddress = findViewById(R.id.tvAddress);
        TextView tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);
        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);

        MaterialButton btnEdit = findViewById(R.id.btnEdit);
        MaterialButton btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        btnBack.setOnClickListener(v -> navigateBack());
        btnEdit.setOnClickListener(v -> finish());

        if (customerName != null) tvCustomerName.setText(customerName);
        if (customerPhone != null) tvCustomerPhone.setText(customerPhone);
        if (service != null) tvService.setText(service);
        if (tvQuantity != null) tvQuantity.setText(String.format(Locale.US, "%s • Qty: %d", (service != null ? service : "Parcel"), quantity));
        String resolvedDate = date != null && !date.trim().isEmpty() ? date.trim() : "ASAP";
        String resolvedTime = time != null ? time.trim() : "";
        String dateTimeLabel = resolvedTime.isEmpty()
                ? "Date & Time: " + resolvedDate
                : "Date & Time: " + resolvedDate + " • " + resolvedTime;
        if (tvDateTime != null) tvDateTime.setText(dateTimeLabel);
        // Pickup is always the fixed TRIMAX Hub; only the delivery side shows the customer's address.
        if (tvAddress != null) tvAddress.setText(BookingRecord.HUB_NAME);
        if (address != null && tvDeliveryAddress != null) tvDeliveryAddress.setText(address);

        // Estimated from the prices shown on the previous step; the server prices the booking when it is saved.
        double total = intent.getDoubleExtra("ESTIMATED_TOTAL", selectedPrice * quantity);
        tvTotalAmount.setText(String.format(Locale.US, "₱ %,.2f", total));
        // Line keys: the product id, or minus it for a half case line (see ItemDiscounts.key).
        final int[] productIds = intent.getIntArrayExtra("PRODUCT_IDS");
        final int[] productQuantities = intent.getIntArrayExtra("PRODUCT_QUANTITIES");

        tvTotal = tvTotalAmount;
        lessFromItems = ItemDiscounts.fromArrays(productIds, intent.getDoubleArrayExtra("PRODUCT_DISCOUNTS"));
        double less = intent.getDoubleExtra("ESTIMATED_DISCOUNT", 0);
        estimatedLess = Double.isFinite(less) && less > 0
                ? BigDecimal.valueOf(less).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        estimatedSubtotal = Double.isFinite(total) && total > 0
                ? BigDecimal.valueOf(total).setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        if (productIds != null && productQuantities != null && productIds.length == productQuantities.length) {
            for (int i = 0; i < productIds.length; i++) {
                quoteLines.add(CreateBookingRequest.Line.ofKey(productIds[i], productQuantities[i]));
            }
        }
        setUpDiscount();

        // A booking needs a customer saved on the server with a photo; offered when the server says so.
        addCustomerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Toast.makeText(this, "Customer saved. Tap Confirm Booking again.", Toast.LENGTH_LONG).show();
                    }
                });

        btnConfirmBooking.setOnClickListener(v -> {
            if (address == null || address.trim().isEmpty()
                    || !Double.isFinite(destLat) || !Double.isFinite(destLng)
                    || destLat < -90 || destLat > 90 || destLng < -180 || destLng > 180
                    || (destLat == 0 && destLng == 0)) {
                Toast.makeText(this, "Please select a valid delivery destination before confirming.", Toast.LENGTH_LONG).show();
                return;
            }
            if (productIds == null || productQuantities == null || productIds.length == 0
                    || productIds.length != productQuantities.length) {
                Toast.makeText(this, "Please go back and select at least 1 product.", Toast.LENGTH_LONG).show();
                return;
            }
            if (submitting) return;   // ignore rapid repeat taps while the first submission is running

            String notes = intent.getStringExtra("NOTES");
            String scheduledTimeStr = resolvedTime.isEmpty()
                    ? resolvedDate
                    : resolvedDate + " • " + resolvedTime;

            // The booking is created on the TRIMAX server: Pending, with no driver. Only an administrator
            // assigns a driver, from the desktop app.
            CreateBookingRequest request = new CreateBookingRequest(requestId);
            request.customerName = customerName;
            request.customerContact = customerPhone;
            request.deliveryAddress = address;
            request.latitude = destLat;
            request.longitude = destLng;
            request.schedule = scheduledTimeStr;
            request.instructions = notes;
            for (int i = 0; i < productIds.length; i++) {
                request.lines.add(CreateBookingRequest.Line.ofKey(productIds[i], productQuantities[i]));
            }
            String discountProblem = itemRows != null ? itemRows.firstProblem() : null;
            if (discountProblem != null) {
                showDiscountError(discountProblem);
                return;
            }
            java.util.Map<Integer, BigDecimal> perUnit = itemRows != null
                    ? ItemDiscounts.nonZero(itemRows.values()) : new java.util.HashMap<>();
            for (CreateBookingRequest.Line l : request.lines) l.discountPerUnit = perUnit.get(l.key());
            String problem = request.validate();
            if (problem != null) {
                Toast.makeText(this, problem, Toast.LENGTH_LONG).show();
                return;
            }

            submitting = true;
            final CharSequence confirmLabel = btnConfirmBooking.getText();
            btnConfirmBooking.setEnabled(false);
            btnConfirmBooking.setText("Submitting...");
            btnEdit.setEnabled(false);
            TrimaxApi.get(this).createBooking(request, new TrimaxApi.Callback<BookingRecord>() {
                @Override
                public void onSuccess(BookingRecord saved) {
                    if (SessionGuard.isGone(NewBookingSummaryActivity.this)) return;
                    SessionManager session = new SessionManager(NewBookingSummaryActivity.this);
                    DatabaseHelper dbHelper = new DatabaseHelper(NewBookingSummaryActivity.this);
                    // Local conveniences only: the saved-customers list and a notification on this phone.
                    dbHelper.saveCustomer(customerName, customerPhone, customerEmail, address, destLat, destLng);
                    dbHelper.addNotification(session.getUsername(), "Booking Request Submitted",
                            "Booking #" + saved.idText() + " is pending admin confirmation.");

                    Intent confirmedIntent = new Intent(NewBookingSummaryActivity.this, BookingConfirmedActivity.class);
                    confirmedIntent.putExtra("BOOKING_ID", saved.idText());
                    startActivity(confirmedIntent);
                    finishAffinity(); // Close creation flow stack
                }

                @Override
                public void onError(ApiException error) {
                    if (SessionGuard.isGone(NewBookingSummaryActivity.this)) return;
                    submitting = false;
                    btnConfirmBooking.setEnabled(true);
                    btnConfirmBooking.setText(confirmLabel);
                    btnEdit.setEnabled(true);
                    if (SessionGuard.handle(NewBookingSummaryActivity.this, error)) return;
                    String detail = error.getDetail();
                    if ("customer_photo_required".equals(detail)) {
                        new androidx.appcompat.app.AlertDialog.Builder(NewBookingSummaryActivity.this)
                                .setTitle("Save the customer first")
                                .setMessage("This customer is not saved yet. Save them with a photo, then confirm "
                                        + "the booking again.")
                                .setPositiveButton("Add customer", (d, w) -> {
                                    Intent add = new Intent(NewBookingSummaryActivity.this, AddCustomerActivity.class);
                                    add.putExtra("CUSTOMER_NAME", customerName);
                                    add.putExtra("CUSTOMER_PHONE", customerPhone);
                                    add.putExtra("CUSTOMER_EMAIL", customerEmail);
                                    add.putExtra("CUSTOMER_ADDRESS", address);
                                    add.putExtra("CUSTOMER_LAT", destLat);
                                    add.putExtra("CUSTOMER_LNG", destLng);
                                    add.putExtra(AddCustomerActivity.EXTRA_NOTICE,
                                            "Take or choose the customer's photo to save them. Then confirm the booking.");
                                    addCustomerLauncher.launch(add);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return;
                    }
                    if ("discount_too_big".equals(detail) || "discount_not_configured".equals(detail)
                            || "discount_not_allowed".equals(detail) || "discount_per_item_only".equals(detail)) {
                        showDiscountError(error.getMessage());
                        loadQuote();   // prices or the minimum may have changed
                        return;
                    }
                    String message = error.getMessage();
                    // A time-out or dropped connection can happen after the server saved the booking.
                    // Confirming again from this screen reuses the same request id, so it cannot duplicate it.
                    boolean unsure = error.getKind() == ApiException.Kind.TIMEOUT
                            || error.getKind() == ApiException.Kind.NETWORK;
                    if (unsure) {
                        message += "\n\nThe booking may already have been saved. Tap Confirm again from this "
                                + "screen (before editing anything): it will not create a second booking.";
                    }
                    new androidx.appcompat.app.AlertDialog.Builder(NewBookingSummaryActivity.this)
                            .setTitle(unsure ? "Not sure it was saved" : "Booking not saved")
                            .setMessage(message)
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        });
    }

    // ------------------------------------------------------------------ discount

    private void setUpDiscount() {
        tvDiscountLimit = findViewById(R.id.tvDiscountLimit);
        tvSubtotalAmount = findViewById(R.id.tvSubtotalAmount);
        tvDiscountAmount = findViewById(R.id.tvDiscountAmount);
        tvDiscountLabel = findViewById(R.id.tvDiscountLabel);
        rowSubtotal = findViewById(R.id.rowSubtotal);
        rowDiscount = findViewById(R.id.rowDiscount);
        android.widget.LinearLayout list = findViewById(R.id.llItemDiscounts);
        if (list == null) return;
        itemRows = new ItemDiscountRows(this, list, this::refreshDiscount);
        if (tvDiscountLimit != null) tvDiscountLimit.setOnClickListener(v -> {
            if (quote == null && !quoteLoading) loadQuote();
        });
        refreshDiscount();
        loadQuote();
    }

    private void loadQuote() {
        if (quoteLines.isEmpty() || quoteLoading) return;
        quoteLoading = true;
        if (tvDiscountLimit != null) tvDiscountLimit.setText("Checking which items can be discounted...");
        TrimaxApi.get(this).discountQuote(quoteLines, new TrimaxApi.Callback<DiscountQuote>() {
            @Override
            public void onSuccess(DiscountQuote result) {
                if (SessionGuard.isGone(NewBookingSummaryActivity.this)) return;
                quoteLoading = false;
                java.util.Map<Integer, BigDecimal> typed = itemRows != null ? itemRows.values() : null;
                if ((typed == null || typed.isEmpty()) && lessFromItems != null && !lessFromItems.isEmpty()) {
                    typed = lessFromItems;      // what the agent picked with "Less" on the item list
                }
                lessFromItems = null;
                quote = result;
                if (itemRows != null) itemRows.show(result.items, typed);
                if (tvDiscountLimit != null) {
                    tvDiscountLimit.setText(result.items.isEmpty()
                            ? "Discounts are not available on this server yet."
                            : ItemDiscounts.summary(result.items));
                }
                refreshDiscount();
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(NewBookingSummaryActivity.this)) return;
                quoteLoading = false;
                if (SessionGuard.handle(NewBookingSummaryActivity.this, error)) return;
                // Without the quote there are no discount boxes: the booking can still be confirmed at full price,
                // so the "Less" estimate from the item list is no longer shown as if it applied.
                estimatedLess = BigDecimal.ZERO;
                refreshDiscount();
                if (tvDiscountLimit != null) {
                    tvDiscountLimit.setText("Could not check which items can be discounted (" + error.getMessage()
                            + "). Tap here to try again.");
                }
            }
        });
    }

    /** Original price, discount and final price while the agent types. */
    private void refreshDiscount() {
        ItemDiscounts.Totals t;
        if (quote != null && !quote.items.isEmpty()) {
            t = ItemDiscounts.totals(quote.items, itemRows != null ? itemRows.values() : null);
        } else {
            t = new ItemDiscounts.Totals();
            t.subtotal = ItemDiscounts.scale(estimatedSubtotal);
            // Until the server's quote arrives, show the "Less" picked on the item list as an estimate.
            if (estimatedLess.compareTo(t.subtotal) <= 0) t.discount = ItemDiscounts.scale(estimatedLess);
        }
        boolean show = t.discount.signum() > 0;
        if (rowSubtotal != null) rowSubtotal.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rowDiscount != null) rowDiscount.setVisibility(show ? View.VISIBLE : View.GONE);
        if (tvSubtotalAmount != null) tvSubtotalAmount.setText(DiscountCalc.peso(t.subtotal));
        if (tvDiscountAmount != null) tvDiscountAmount.setText("- " + DiscountCalc.peso(t.discount));
        if (tvDiscountLabel != null) tvDiscountLabel.setText("Discount on items");
        if (tvTotal != null) tvTotal.setText(String.format(Locale.US, "₱ %,.2f", t.total()));
    }

    private void showDiscountError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        if (tvDiscountLimit != null) tvDiscountLimit.setText(message);
    }

    private void navigateBack() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
