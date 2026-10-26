package com.example.trimax;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;
import java.util.Locale;

public class ClientDetailActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private LinearLayout clientBookingHistoryContainer;
    private String photoPath;
    private androidx.activity.result.ActivityResultLauncher<Intent> photoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_client_detail);

        dbHelper = new DatabaseHelper(this);

        // Keep screen content clear of the status and navigation bars.
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        String name = intent.getStringExtra("CLIENT_NAME");
        String phone = intent.getStringExtra("CLIENT_PHONE");
        String email = intent.getStringExtra("CLIENT_EMAIL");
        String address = intent.getStringExtra("CLIENT_ADDRESS");

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvClientName = findViewById(R.id.tvClientName);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvEmail = findViewById(R.id.tvEmail);
        TextView tvAddress = findViewById(R.id.tvAddress);
        clientBookingHistoryContainer = findViewById(R.id.clientBookingHistoryContainer);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBack();
            }
        });

        if (name != null) tvClientName.setText(name);
        if (phone != null) tvPhone.setText(String.format("📞 %s", phone));
        if (email != null && !email.trim().isEmpty()) tvEmail.setText(String.format("✉ %s", email));
        else if (tvEmail != null) tvEmail.setVisibility(View.GONE);
        if (tvAddress != null) tvAddress.setText(address != null && !address.trim().isEmpty()
                ? "📍 " + address : "📍 Address not saved yet");

        // Phone call intent action
        if (tvPhone != null) {
            tvPhone.setOnClickListener(v -> {
                String cleanPhone = phone != null ? phone.replaceAll("[^0-9]", "") : "09171234567";
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + cleanPhone));
                startActivity(dialIntent);
            });
        }

        // Email intent action
        if (tvEmail != null) {
            tvEmail.setOnClickListener(v -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + (email != null ? email : "juan@gmail.com")));
                startActivity(emailIntent);
            });
        }

        photoPath = intent.getStringExtra("CLIENT_PHOTO_PATH");
        double lat = intent.getDoubleExtra("CLIENT_LAT", 0), lng = intent.getDoubleExtra("CLIENT_LNG", 0);
        photoLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != RESULT_OK) return;
                    CustomerAvatars.forget(phone);
                    photoPath = null;   // the new photo is on this phone now
                    showPhoto(phone, false);
                });
        View btnPhoto = findViewById(R.id.btnClientPhoto);
        if (btnPhoto != null) btnPhoto.setOnClickListener(v -> {
            Intent edit = new Intent(this, AddCustomerActivity.class);
            edit.putExtra("CUSTOMER_NAME", name);
            edit.putExtra("CUSTOMER_PHONE", phone);
            edit.putExtra("CUSTOMER_EMAIL", email);
            edit.putExtra("CUSTOMER_ADDRESS", address);
            edit.putExtra("CUSTOMER_LAT", lat);
            edit.putExtra("CUSTOMER_LNG", lng);
            edit.putExtra(AddCustomerActivity.EXTRA_NOTICE,
                    "Take or choose a photo of this customer, check the details, then save.");
            photoLauncher.launch(edit);
        });
        showPhoto(phone, false);

        loadBookingHistory(phone, name);
    }

    /** The customer's photo (this phone, else the server), a placeholder, or an error with Retry. */
    private void showPhoto(String phone, boolean retry) {
        ImageView iv = findViewById(R.id.ivClientPhoto);
        TextView status = findViewById(R.id.tvClientPhotoStatus);
        View spinner = findViewById(R.id.progressClientPhoto);
        com.google.android.material.button.MaterialButton btn = findViewById(R.id.btnClientPhoto);
        CustomerAvatars.bind(this, iv, phone, photoPath, dp(192), retry, (state, message) -> {
            if (spinner != null) spinner.setVisibility(state == CustomerAvatars.State.LOADING ? View.VISIBLE : View.GONE);
            if (status != null) {
                status.setOnClickListener(null);
                if (state == CustomerAvatars.State.ERROR) {
                    status.setText((message == null ? "The photo could not be loaded." : message) + "  Tap to retry.");
                    status.setOnClickListener(v -> showPhoto(phone, true));
                    status.setVisibility(View.VISIBLE);
                } else if (state == CustomerAvatars.State.NO_PHOTO) {
                    status.setText("No photo yet (saved before photos were required).");
                    status.setVisibility(View.VISIBLE);
                } else {
                    status.setVisibility(View.GONE);
                }
            }
            if (btn != null) {
                boolean canAdd = CustomerPhotos.key(phone) != null && state != CustomerAvatars.State.LOADING;
                btn.setVisibility(canAdd ? View.VISIBLE : View.GONE);
                btn.setText(state == CustomerAvatars.State.SHOWN ? "Update photo" : "Add photo");
            }
        });
    }

    /** This customer's bookings, from the agent's own bookings on the TRIMAX server. */
    private void loadBookingHistory(String phone, String name) {
        if (clientBookingHistoryContainer == null) return;
        showHistoryNote("Loading booking history...");
        TrimaxApi.get(this).listMyBookings(new TrimaxApi.Callback<List<BookingRecord>>() {
            @Override
            public void onSuccess(List<BookingRecord> records) {
                if (SessionGuard.isGone(ClientDetailActivity.this)) return;
                List<Delivery> bookings = new java.util.ArrayList<>();
                for (BookingRecord r : BookingFilter.forCustomer(records, phone, name)) bookings.add(r.toDelivery(false));
                showBookingHistory(bookings);
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(ClientDetailActivity.this)) return;
                if (SessionGuard.handle(ClientDetailActivity.this, error)) return;
                showHistoryNote("Could not load booking history: " + error.getMessage());
            }
        });
    }

    private void showHistoryNote(String text) {
        clientBookingHistoryContainer.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tv.setTextSize(14);
        tv.setPadding(dp(4), dp(12), dp(4), dp(12));
        clientBookingHistoryContainer.addView(tv);
    }

    private void showBookingHistory(List<Delivery> bookings) {
        clientBookingHistoryContainer.removeAllViews();
        if (bookings == null || bookings.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No booking history found for this client.");
            tvEmpty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvEmpty.setTextSize(14);
            tvEmpty.setPadding(dp(4), dp(12), dp(4), dp(12));
            clientBookingHistoryContainer.addView(tvEmpty);
            return;
        }

        for (Delivery d : bookings) {
            CardView card = new CardView(this);
            CardView.LayoutParams cardParams = new CardView.LayoutParams(
                    CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cardParams);
            card.setRadius(16);
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg));
            card.setCardElevation(2);
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(dp(16), dp(16), dp(16), dp(16));

            TextView tvCode = new TextView(this);
            tvCode.setText(String.format("Booking #%s", d.getBookingId()));
            tvCode.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvCode.setTextSize(15);
            tvCode.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvDest = new TextView(this);
            tvDest.setText(String.format("Destination: %s", d.getDestination()));
            tvDest.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvDest.setTextSize(13);
            tvDest.setPadding(0, dp(4), 0, 0);

            TextView tvStatus = new TextView(this);
            tvStatus.setText(String.format("Status: %s • ₱ %.2f", d.getStatus(), d.getCurrentTotal()));
            tvStatus.setTextColor(ContextCompat.getColor(this, "DELIVERED".equalsIgnoreCase(d.getStatus()) || "COMPLETED".equalsIgnoreCase(d.getStatus()) ? R.color.status_delivered : R.color.primary_blue));
            tvStatus.setTextSize(13);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setPadding(0, dp(6), 0, 0);

            layout.addView(tvCode);
            layout.addView(tvDest);
            layout.addView(tvStatus);
            card.addView(layout);

            card.setOnClickListener(v -> openBookingDetail(d.getBookingId(), d.getCustomerName(), d.getStatus(), String.format(Locale.US, "₱ %.2f", d.getCurrentTotal())));

            clientBookingHistoryContainer.addView(card);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void openBookingDetail(String id, String customer, String status, String amount) {
        Intent intent = new Intent(ClientDetailActivity.this, BookingDetailActivity.class);
        intent.putExtra("BOOKING_ID", id);
        intent.putExtra("CUSTOMER_NAME", customer);
        intent.putExtra("STATUS", status);
        intent.putExtra("TOTAL_AMOUNT", amount);
        startActivity(intent);
    }

    private void navigateBack() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
