package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClientsActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private LinearLayout clientsListContainer;
    private TextInputEditText etSearch;
    /** The agent's bookings on the server (null until loaded): customers booked from any phone. */
    private List<BookingRecord> serverBookings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clients);
        dbHelper = new DatabaseHelper(this);
        clientsListContainer = findViewById(R.id.clientsListContainer);
        etSearch = findViewById(R.id.etSearch);

        View root = findViewById(R.id.topBar) != null
                ? (View) findViewById(R.id.topBar).getParent()
                : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), 0);
                return insets;
            });
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

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    renderCustomers(s != null ? s.toString() : "");
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        renderCustomers("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderCustomers(currentQuery());
        TrimaxApi.get(this).listMyBookings(new TrimaxApi.Callback<List<BookingRecord>>() {
            @Override
            public void onSuccess(List<BookingRecord> records) {
                if (SessionGuard.isGone(ClientsActivity.this)) return;
                serverBookings = records;
                renderCustomers(currentQuery());
                if (clientsListContainer != null) clientsListContainer.scheduleLayoutAnimation();
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(ClientsActivity.this)) return;
                SessionGuard.handle(ClientsActivity.this, error);
                // Offline: the customers saved on this phone are still listed.
            }
        });
    }

    private String currentQuery() {
        return etSearch != null && etSearch.getText() != null ? etSearch.getText().toString() : "";
    }


    private void renderCustomers(String query) {
        if (clientsListContainer == null || dbHelper == null) return;
        clientsListContainer.removeAllViews();
        com.google.android.material.button.MaterialButton addCustomer = new com.google.android.material.button.MaterialButton(this);
        addCustomer.setText("+ Add New Customer");
        addCustomer.setAllCaps(false);
        addCustomer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.card_bg)));
        addCustomer.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        addParams.bottomMargin = dp(14);
        clientsListContainer.addView(addCustomer, addParams);
        addCustomer.setOnClickListener(v -> {
            SessionManager session = new SessionManager(this);
            if (dbHelper == null) dbHelper = new DatabaseHelper(this);
            boolean isClockedIn = session.isDutyOnline() || dbHelper.getActiveAttendance(session.getUsername()) != null;
            if (!isClockedIn) {
                new AlertDialog.Builder(ClientsActivity.this)
                        .setTitle("Attendance Required")
                        .setMessage("You must be clocked in (Start Duty / Agent Attendance) before adding a new customer or booking.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
            Intent intent = new Intent(this, NewBookingCustomerActivity.class);
            intent.putExtra("CUSTOMER_DIRECTORY_ONLY", true);
            startActivity(intent);
        });
        List<DatabaseHelper.SavedCustomer> customers = CustomerDirectory.list(dbHelper, serverBookings, query);
        if (customers.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(query == null || query.trim().isEmpty()
                    ? "No saved customers yet. Add a customer from the New Booking screen."
                    : "No customers match your search.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setTextSize(14);
            empty.setPadding(dp(8), dp(20), dp(8), dp(20));
            clientsListContainer.addView(empty);
            return;
        }

        for (DatabaseHelper.SavedCustomer customer : customers) {
            clientsListContainer.addView(createCustomerCard(customer));
        }
    }

    private CardView createCustomerCard(DatabaseHelper.SavedCustomer customer) {
        CardView card = new CardView(this);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg));
        card.setRadius(dp(16));
        card.setCardElevation(dp(2));
        card.setUseCompatPadding(false);
        card.setClickable(true);
        card.setFocusable(true);
        card.setForeground(getDrawable(android.R.drawable.list_selector_background));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView avatar = new TextView(this);
        avatar.setText(customer.getName() == null || customer.getName().trim().isEmpty()
                ? "?" : customer.getName().trim().substring(0, 1).toUpperCase());
        avatar.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
        avatar.setTextSize(18);
        avatar.setGravity(android.view.Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_icon_badge);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        avatarParams.rightMargin = dp(14);
        row.addView(avatar, avatarParams);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView name = new TextView(this);
        name.setText(customer.getName());
        name.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        name.setTextSize(16);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        details.addView(name);

        TextView phone = new TextView(this);
        phone.setText("☎ " + safe(customer.getPhone()));
        phone.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        phone.setTextSize(13);
        LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        phoneParams.topMargin = dp(3);
        details.addView(phone, phoneParams);

        if (customer.getEmail() != null && !customer.getEmail().trim().isEmpty()) {
            TextView email = new TextView(this);
            email.setText(customer.getEmail());
            email.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            email.setTextSize(13);
            details.addView(email);
        }

        TextView address = new TextView(this);
        address.setText(customer.getAddress() == null || customer.getAddress().trim().isEmpty()
                ? "Address not saved yet" : customer.getAddress());
        address.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        address.setTextSize(12);
        address.setMaxLines(2);
        LinearLayout.LayoutParams addressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        addressParams.topMargin = dp(4);
        details.addView(address, addressParams);
        row.addView(details);

        TextView bookings = new TextView(this);
        bookings.setText(customer.getBookingCount() + " bookings");
        bookings.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
        bookings.setTextSize(11);
        bookings.setPadding(dp(8), dp(5), dp(8), dp(5));
        row.addView(bookings);
        card.addView(row);
        card.setOnClickListener(v -> openClientDetail(customer));
        return card;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "No phone" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void openClientDetail(DatabaseHelper.SavedCustomer customer) {
        Intent intent = new Intent(this, ClientDetailActivity.class);
        intent.putExtra("CLIENT_NAME", customer.getName());
        intent.putExtra("CLIENT_PHONE", customer.getPhone());
        intent.putExtra("CLIENT_EMAIL", customer.getEmail());
        intent.putExtra("CLIENT_ADDRESS", customer.getAddress());
        intent.putExtra("BOOKINGS_COUNT", String.valueOf(customer.getBookingCount()));
        startActivity(intent);
    }

    private void navigateToTab(Class<?> targetClass) {
        Intent intent = new Intent(ClientsActivity.this, targetClass);
        intent.putExtra("PREV_TAB_INDEX", 3);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    private void setupBottomNavigation() {
        highlightBottomNavItem(R.id.nav_clients);

        View home = findViewById(R.id.nav_home);
        View bookings = findViewById(R.id.nav_bookings);
        View packages = findViewById(R.id.nav_packages);
        View clients = findViewById(R.id.nav_clients);
        View profile = findViewById(R.id.nav_profile);

        if (home != null) home.setOnClickListener(v -> navigateToTab(DashboardActivity.class));
        if (bookings != null) bookings.setOnClickListener(v -> navigateToTab(BookingsActivity.class));
        if (packages != null) packages.setOnClickListener(v -> navigateToTab(PackagesActivity.class));
        if (clients != null) clients.setOnClickListener(v -> highlightBottomNavItem(R.id.nav_clients));
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
}
