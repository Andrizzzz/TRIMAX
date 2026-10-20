package com.example.trimax;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class NewBookingCustomerActivity extends AppCompatActivity {

    private TextInputEditText etCustomerName, etPhone, etEmail, etCustomerAddress;
    private DatabaseHelper dbHelper;
    private String selectedCustomerAddress = "";
    private double selectedCustomerLatitude = 0, selectedCustomerLongitude = 0;
    private boolean directoryOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_booking_customer);
        dbHelper = new DatabaseHelper(this);
        directoryOnly = getIntent().getBooleanExtra("CUSTOMER_DIRECTORY_ONLY", false);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        etCustomerName = findViewById(R.id.etCustomerName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etCustomerAddress = findViewById(R.id.etCustomerAddress);
        if (etCustomerAddress != null) {
            etCustomerAddress.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String currentAddress = s != null ? s.toString().trim() : "";
                    if (!currentAddress.equals(selectedCustomerAddress)) {
                        selectedCustomerLatitude = 0;
                        selectedCustomerLongitude = 0;
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        // Populate real saved customers and keep inline selection mutually exclusive.
        configureInlineCustomerSelection();

        MaterialButton btnChooseCustomer = findViewById(R.id.btnChooseCustomer);
        MaterialButton btnAddCustomer = findViewById(R.id.btnAddCustomer);
        MaterialButton btnSaveCustomer = findViewById(R.id.btnSaveCustomer);
        MaterialButton btnNext = findViewById(R.id.btnNext);
        if (directoryOnly) {
            if (btnNext != null) btnNext.setVisibility(View.GONE);
        }

        if (btnBack != null) btnBack.setOnClickListener(v -> navigateBack());
        if (btnChooseCustomer != null) btnChooseCustomer.setOnClickListener(v -> showSavedCustomers());
        if (btnAddCustomer != null) btnAddCustomer.setOnClickListener(v -> {
            etCustomerName.setText("");
            etPhone.setText("");
            etEmail.setText("");
            if (etCustomerAddress != null) etCustomerAddress.setText("");
            selectedCustomerAddress = "";
            selectedCustomerLatitude = 0;
            selectedCustomerLongitude = 0;
            etCustomerName.requestFocus();
            Toast.makeText(this, "Enter the new customer's information.", Toast.LENGTH_SHORT).show();
        });
        if (btnSaveCustomer != null) btnSaveCustomer.setOnClickListener(v -> saveCustomerFromForm());

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                String name = etCustomerName != null && etCustomerName.getText() != null ? etCustomerName.getText().toString().trim() : "";
                String phone = etPhone != null && etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                String email = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

                if (name.isEmpty()) {
                    if (etCustomerName != null) {
                        etCustomerName.setError("Customer name is required");
                        etCustomerName.requestFocus();
                    }
                } else if (phone.isEmpty() || !Patterns.PHONE.matcher(phone).matches() || phone.length() < 7) {
                    if (etPhone != null) {
                        etPhone.setError("Valid phone number is required");
                        etPhone.requestFocus();
                    }
                } else if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    if (etEmail != null) {
                        etEmail.setError("Valid email address is required");
                        etEmail.requestFocus();
                    }
                } else {
                    btnNext.setEnabled(false);
                    selectedCustomerAddress = etCustomerAddress != null && etCustomerAddress.getText() != null
                            ? etCustomerAddress.getText().toString().trim() : selectedCustomerAddress;
                    dbHelper.saveCustomer(name, phone, email, selectedCustomerAddress, selectedCustomerLatitude, selectedCustomerLongitude);

                    Intent intent = new Intent(NewBookingCustomerActivity.this, NewBookingDetailsActivity.class);
                    intent.putExtra("CUSTOMER_NAME", name);
                    intent.putExtra("CUSTOMER_PHONE", phone);
                    intent.putExtra("CUSTOMER_EMAIL", email);
                    intent.putExtra("CUSTOMER_ADDRESS", selectedCustomerAddress);
                    intent.putExtra("CUSTOMER_LAT", selectedCustomerLatitude);
                    intent.putExtra("CUSTOMER_LNG", selectedCustomerLongitude);
                    if (getIntent().hasExtra("SELECTED_PACKAGE")) {
                        intent.putExtra("SELECTED_PACKAGE", getIntent().getStringExtra("SELECTED_PACKAGE"));
                        intent.putExtra("SELECTED_PRICE", getIntent().getDoubleExtra("SELECTED_PRICE", 99.00));
                    }
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MaterialButton btnNext = findViewById(R.id.btnNext);
        if (btnNext != null) {
            btnNext.setEnabled(true);
        }
    }

    private void configureInlineCustomerSelection() {
        int[] cardIds = {R.id.cardCustomer1, R.id.cardCustomer2, R.id.cardCustomer3};
        int[] nameIds = {R.id.tvCust1Name, R.id.tvCust2Name, R.id.tvCust3Name};
        int[] phoneIds = {R.id.tvCust1Phone, R.id.tvCust2Phone, R.id.tvCust3Phone};
        int[] radioIds = {R.id.rbCust1, R.id.rbCust2, R.id.rbCust3};
        List<DatabaseHelper.SavedCustomer> customers = dbHelper.getSavedCustomers("");
        View container = findViewById(R.id.containerCustomerList);
        View cardContainer = container != null ? (View) container.getParent() : null;

        for (int i = 0; i < cardIds.length; i++) {
            View card = findViewById(cardIds[i]);
            TextView nameView = findViewById(nameIds[i]);
            TextView phoneView = findViewById(phoneIds[i]);
            android.widget.RadioButton radio = findViewById(radioIds[i]);
            if (card == null || nameView == null || phoneView == null || radio == null) continue;

            if (customers == null || i >= customers.size()) {
                card.setVisibility(View.GONE);
                radio.setChecked(false);
                continue;
            }

            DatabaseHelper.SavedCustomer customer = customers.get(i);
            card.setVisibility(View.VISIBLE);
            nameView.setText(customer.getName());
            phoneView.setText(customer.getPhone() == null ? "" : customer.getPhone());
            radio.setChecked(false);

            View.OnClickListener selectCustomer = v -> {
                for (int radioId : radioIds) {
                    android.widget.RadioButton other = findViewById(radioId);
                    if (other != null) other.setChecked(false);
                }
                radio.setChecked(true);

                if (etCustomerName != null) etCustomerName.setText(customer.getName());
                if (etPhone != null) etPhone.setText(customer.getPhone());
                if (etEmail != null) etEmail.setText(customer.getEmail());
                selectedCustomerAddress = customer.getAddress() == null ? "" : customer.getAddress();
                selectedCustomerLatitude = customer.getLatitude();
                selectedCustomerLongitude = customer.getLongitude();
                if (etCustomerAddress != null) etCustomerAddress.setText(selectedCustomerAddress);
                Toast.makeText(this, "Selected customer: " + customer.getName(), Toast.LENGTH_SHORT).show();
            };
            card.setOnClickListener(selectCustomer);
            radio.setOnClickListener(selectCustomer);
        }

        if (cardContainer != null) {
            cardContainer.setVisibility(customers == null || customers.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void showSavedCustomers() {
        List<DatabaseHelper.SavedCustomer> allCustomers = dbHelper.getSavedCustomers("");
        if (allCustomers.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Saved Customers")
                    .setMessage("No saved customers yet. Enter details below and tap 'Save Customer to Directory'.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_saved_customer, null);
        bottomSheetDialog.setContentView(dialogView);

        ImageView btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
        TextInputEditText etSearchCustomer = dialogView.findViewById(R.id.etSearchCustomer);
        LinearLayout containerSavedCustomers = dialogView.findViewById(R.id.containerSavedCustomers);

        if (btnCloseDialog != null) {
            btnCloseDialog.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        populateSavedCustomerList(containerSavedCustomers, allCustomers, bottomSheetDialog);

        if (etSearchCustomer != null) {
            etSearchCustomer.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s != null ? s.toString().trim() : "";
                    List<DatabaseHelper.SavedCustomer> filtered = dbHelper.getSavedCustomers(query);
                    populateSavedCustomerList(containerSavedCustomers, filtered, bottomSheetDialog);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        bottomSheetDialog.show();
    }

    private void populateSavedCustomerList(LinearLayout container, List<DatabaseHelper.SavedCustomer> customers, BottomSheetDialog dialog) {
        if (container == null) return;
        container.removeAllViews();

        if (customers == null || customers.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No matching saved customers found.");
            tvEmpty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvEmpty.setTextSize(14);
            tvEmpty.setPadding(dp(8), dp(24), dp(8), dp(24));
            tvEmpty.setGravity(Gravity.CENTER);
            container.addView(tvEmpty);
            return;
        }

        for (DatabaseHelper.SavedCustomer customer : customers) {
            CardView card = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dp(12));
            card.setLayoutParams(cardParams);
            card.setRadius(dp(16));
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg));
            card.setCardElevation(dp(2));
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.CENTER_VERTICAL);
            layout.setPadding(dp(16), dp(16), dp(16), dp(16));

            // Avatar Icon Badge
            ImageView avatar = new ImageView(this);
            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(44), dp(44));
            avatarParams.setMarginEnd(dp(14));
            avatar.setLayoutParams(avatarParams);
            avatar.setImageResource(R.drawable.ic_user);
            avatar.setBackgroundResource(R.drawable.bg_icon_badge);
            avatar.setPadding(dp(10), dp(10), dp(10), dp(10));
            avatar.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_blue)));

            // Text Info Container
            LinearLayout textContainer = new LinearLayout(this);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            textContainer.setLayoutParams(textParams);

            TextView tvName = new TextView(this);
            tvName.setText(customer.getName());
            tvName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvName.setTextSize(16);
            tvName.setTypeface(null, Typeface.BOLD);

            TextView tvPhone = new TextView(this);
            tvPhone.setText("📞 " + customer.getPhone());
            tvPhone.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
            tvPhone.setTextSize(13);
            tvPhone.setPadding(0, dp(2), 0, 0);

            textContainer.addView(tvName);
            textContainer.addView(tvPhone);

            if (customer.getAddress() != null && !customer.getAddress().trim().isEmpty()) {
                TextView tvAddr = new TextView(this);
                tvAddr.setText("📍 " + customer.getAddress());
                tvAddr.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                tvAddr.setTextSize(12);
                tvAddr.setMaxLines(2);
                tvAddr.setEllipsize(TextUtils.TruncateAt.END);
                tvAddr.setPadding(0, dp(2), 0, 0);
                textContainer.addView(tvAddr);
            }

            // Booking Count Chip
            TextView tvBadge = new TextView(this);
            tvBadge.setText(customer.getBookingCount() + " booking" + (customer.getBookingCount() == 1 ? "" : "s"));
            tvBadge.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
            tvBadge.setTextSize(11);
            tvBadge.setTypeface(null, Typeface.BOLD);
            tvBadge.setBackgroundResource(R.drawable.bg_pill_selected);
            tvBadge.setPadding(dp(8), dp(4), dp(8), dp(4));

            layout.addView(avatar);
            layout.addView(textContainer);
            layout.addView(tvBadge);
            card.addView(layout);

            card.setOnClickListener(v -> {
                if (etCustomerName != null) etCustomerName.setText(customer.getName());
                if (etPhone != null) etPhone.setText(customer.getPhone());
                if (etEmail != null) etEmail.setText(customer.getEmail());
                selectedCustomerAddress = customer.getAddress() != null ? customer.getAddress() : "";
                selectedCustomerLatitude = customer.getLatitude();
                selectedCustomerLongitude = customer.getLongitude();
                if (etCustomerAddress != null) etCustomerAddress.setText(selectedCustomerAddress);
                Toast.makeText(this, "Selected customer: " + customer.getName(), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            container.addView(card);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void saveCustomerFromForm() {
        String name = etCustomerName.getText() != null ? etCustomerName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String address = etCustomerAddress != null && etCustomerAddress.getText() != null
                ? etCustomerAddress.getText().toString().trim() : "";
        if (name.isEmpty()) { etCustomerName.setError("Customer name is required"); etCustomerName.requestFocus(); return; }
        if (phone.isEmpty() || !Patterns.PHONE.matcher(phone).matches() || phone.length() < 7) {
            etPhone.setError("Valid phone number is required"); etPhone.requestFocus(); return;
        }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email address is required"); etEmail.requestFocus(); return;
        }
        if (dbHelper.saveCustomer(name, phone, email, address, selectedCustomerLatitude, selectedCustomerLongitude)) {
            selectedCustomerAddress = address;
            Toast.makeText(this, "Customer saved to directory.", Toast.LENGTH_SHORT).show();
            if (directoryOnly) finish();
        } else {
            Toast.makeText(this, "Unable to save customer. Check the required details.", Toast.LENGTH_LONG).show();
        }
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
