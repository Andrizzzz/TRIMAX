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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    /** Phone digits of the customer the pin above belongs to; a different phone means "no pin yet". */
    private String pinOwnerPhoneDigits = "";
    /** The agent's bookings on the server (null until loaded): customers booked from any phone. */
    private List<BookingRecord> serverBookings;
    /** The agent's customers saved on the server (null until loaded); a booking needs one of these. */
    private List<ServerCustomer> serverCustomers;
    private boolean directoryOnly = false;
    /** Go on to the next step once the customer has been saved with a photo. */
    private boolean continueAfterAdd = false;
    private static final String STATE_CONTINUE_AFTER_ADD = "continue_after_add";
    private ActivityResultLauncher<Intent> addCustomerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        directoryOnly = getIntent().getBooleanExtra("CUSTOMER_DIRECTORY_ONLY", false);
        // Survives the screen being recreated while Add customer is open (camera, low memory, rotation).
        if (savedInstanceState != null) continueAfterAdd = savedInstanceState.getBoolean(STATE_CONTINUE_AFTER_ADD, false);

        // Prevent bypassing the dashboard's duty check through direct navigation.
        // The same rule as the Bookings screen's New Booking button (server duty status or an open
        // attendance on this phone). A stricter check here closed this screen when Android recreated it
        // (for example after the camera in Add customer) and dropped the agent back on Bookings.
        SessionManager dutySession = new SessionManager(this);
        boolean onDuty = dutySession.isDutyOnline()
                || new DatabaseHelper(this).getActiveAttendance(dutySession.getUsername()) != null;
        if (!directoryOnly && !onDuty) {
            new AlertDialog.Builder(this)
                    .setTitle("Attendance Required")
                    .setMessage("You must be clocked in and on duty before creating a new booking. Please start duty first.")
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .setOnCancelListener(dialog -> finish())
                    .show();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_booking_customer);
        dbHelper = new DatabaseHelper(this);

        addCustomerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    boolean thenContinue = continueAfterAdd;
                    continueAfterAdd = false;
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    Intent data = result.getData();
                    if (directoryOnly) {
                        finish();   // Add customer already confirmed the save
                        return;
                    }
                    rememberServerCustomer(data.getStringExtra("CUSTOMER_NAME"), data.getStringExtra("CUSTOMER_PHONE"));
                    loadServerCustomers();
                    configureInlineCustomerSelection();
                    String name = data.getStringExtra("CUSTOMER_NAME");
                    String phone = data.getStringExtra("CUSTOMER_PHONE");
                    String email = data.getStringExtra("CUSTOMER_EMAIL");
                    selectedCustomerAddress = data.getStringExtra("CUSTOMER_ADDRESS");
                    if (selectedCustomerAddress == null) selectedCustomerAddress = "";
                    selectedCustomerLatitude = data.getDoubleExtra("CUSTOMER_LAT", 0);
                    selectedCustomerLongitude = data.getDoubleExtra("CUSTOMER_LNG", 0);
                    pinOwnerPhoneDigits = CustomerDirectory.digits(phone);
                    if (etCustomerName != null) etCustomerName.setText(name);
                    if (etPhone != null) etPhone.setText(phone);
                    if (etEmail != null) etEmail.setText(email);
                    if (etCustomerAddress != null) etCustomerAddress.setText(selectedCustomerAddress);
                    LinearLayout form = findViewById(R.id.llNewCustomerForm);
                    if (form != null) form.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Customer saved and selected.", Toast.LENGTH_SHORT).show();
                    if (thenContinue) goNext();
                });

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
        TextInputEditText etSearchCustomer = findViewById(R.id.etSearchCustomer);
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
        if (etSearchCustomer != null) {
            etSearchCustomer.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s != null ? s.toString().trim() : "";
                    configureInlineCustomerSelection(query);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        // Populate real saved customers and keep inline selection mutually exclusive.
        configureInlineCustomerSelection("");
        // Add the customers this agent booked on the server (other phones, reinstalls).
        TrimaxApi.get(this).listMyBookings(new TrimaxApi.Callback<List<BookingRecord>>() {
            @Override
            public void onSuccess(List<BookingRecord> records) {
                if (SessionGuard.isGone(NewBookingCustomerActivity.this)) return;
                serverBookings = records;
                String q = etSearchCustomer != null && etSearchCustomer.getText() != null
                        ? etSearchCustomer.getText().toString().trim() : "";
                configureInlineCustomerSelection(q);
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(NewBookingCustomerActivity.this)) return;
                SessionGuard.handle(NewBookingCustomerActivity.this, error);
                // Offline: the customers saved on this phone are still listed.
            }
        });
        loadServerCustomers();

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
            // In the booking flow a new customer is added in order to book for them: after saving, go on
            // to the next step (Location, already filled with the customer's pin) instead of stopping here.
            continueAfterAdd = !directoryOnly;
            Intent addIntent = new Intent(NewBookingCustomerActivity.this, AddCustomerActivity.class);
            if (!directoryOnly) {
                addIntent.putExtra(AddCustomerActivity.EXTRA_NOTICE,
                        "Save the customer with a photo and location. The booking continues after saving.");
            }
            addCustomerLauncher.launch(addIntent);
        });
        if (btnSaveCustomer != null) btnSaveCustomer.setOnClickListener(v -> saveCustomerFromForm());

        if (btnNext != null) btnNext.setOnClickListener(v -> goNext());
    }

    /** Checks the customer and goes on to the location step (a customer not on the server is saved first). */
    private void goNext() {
        MaterialButton btnNext = findViewById(R.id.btnNext);
        String name = etCustomerName != null && etCustomerName.getText() != null ? etCustomerName.getText().toString().trim() : "";
        String phone = etPhone != null && etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String email = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (name.isEmpty()) {
            if (etCustomerName != null) {
                etCustomerName.setError("Customer name is required");
                etCustomerName.requestFocus();
            }
            return;
        }
        if (phone.isEmpty() || cleanPhone.length() < 7) {
            if (etPhone != null) {
                etPhone.setError("Valid phone number is required");
                etPhone.requestFocus();
            }
            return;
        }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (etEmail != null) {
                etEmail.setError("Valid email address is required");
                etEmail.requestFocus();
            }
            return;
        }
        selectedCustomerAddress = etCustomerAddress != null && etCustomerAddress.getText() != null
                ? etCustomerAddress.getText().toString().trim() : selectedCustomerAddress;
        // The pin travels only with the customer it was saved for. If the phone was changed to
        // someone else after picking a customer, start without a pin (the next step asks for one).
        if (!cleanPhone.equals(pinOwnerPhoneDigits)) {
            selectedCustomerLatitude = 0;
            selectedCustomerLongitude = 0;
        }
        // A booking needs a customer saved on the server with a photo. When the list is known and this
        // customer is not in it, save them first (with a photo), then continue. When the list could not be
        // loaded (offline, older server), go on: the server checks again when the booking is confirmed.
        if (serverCustomers != null && !CustomerDirectory.onServer(serverCustomers, name, phone)) {
            continueAfterAdd = true;
            openAddCustomer(name, phone, email,
                    "New customer: take or choose their photo and confirm the location to save them. "
                            + "The booking continues after saving.");
            return;
        }
        if (btnNext != null) btnNext.setEnabled(false);
        dbHelper.saveCustomer(name, phone, email, selectedCustomerAddress, selectedCustomerLatitude, selectedCustomerLongitude);

        Intent intent = new Intent(NewBookingCustomerActivity.this, NewBookingLocationActivity.class);
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

    /** Opens Add customer prefilled with what is on this screen. */
    private void openAddCustomer(String name, String phone, String email, String notice) {
        Intent addIntent = new Intent(this, AddCustomerActivity.class);
        addIntent.putExtra("CUSTOMER_NAME", name);
        addIntent.putExtra("CUSTOMER_PHONE", phone);
        addIntent.putExtra("CUSTOMER_EMAIL", email);
        if (CustomerDirectory.digits(phone).equals(pinOwnerPhoneDigits)) {
            addIntent.putExtra("CUSTOMER_ADDRESS", selectedCustomerAddress);
            addIntent.putExtra("CUSTOMER_LAT", selectedCustomerLatitude);
            addIntent.putExtra("CUSTOMER_LNG", selectedCustomerLongitude);
        }
        if (notice != null) addIntent.putExtra(AddCustomerActivity.EXTRA_NOTICE, notice);
        addCustomerLauncher.launch(addIntent);
    }

    /** The agent's customers on the server (with photos); refreshes the lists when they arrive. */
    private void loadServerCustomers() {
        TrimaxApi.get(this).listCustomers(new TrimaxApi.Callback<List<ServerCustomer>>() {
            @Override
            public void onSuccess(List<ServerCustomer> result) {
                if (SessionGuard.isGone(NewBookingCustomerActivity.this)) return;
                serverCustomers = result;
                TextInputEditText search = findViewById(R.id.etSearchCustomer);
                configureInlineCustomerSelection(search != null && search.getText() != null
                        ? search.getText().toString().trim() : "");
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(NewBookingCustomerActivity.this)) return;
                SessionGuard.handle(NewBookingCustomerActivity.this, error);
                // Older server or offline: keep going; the server checks the customer at booking time.
            }
        });
    }

    /** Until the list is reloaded: the customer just saved counts as on the server. */
    private void rememberServerCustomer(String name, String phone) {
        if (serverCustomers == null) return;
        ServerCustomer sc = new ServerCustomer();
        sc.name = name;
        sc.contact = phone;
        serverCustomers = new java.util.ArrayList<>(serverCustomers);
        serverCustomers.add(sc);
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
        configureInlineCustomerSelection("");
    }

    private void configureInlineCustomerSelection(String query) {
        LinearLayout container = findViewById(R.id.containerCustomerList);
        if (container == null) return;

        // Replace static sample rows with saved customers matching search query.
        container.removeAllViews();
        List<DatabaseHelper.SavedCustomer> customers = CustomerDirectory.list(dbHelper, serverBookings, serverCustomers, query != null ? query : "");
        if (customers == null || customers.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(TextUtils.isEmpty(query)
                    ? "No saved customers yet. Tap + Add New Customer to create one."
                    : "No matching saved customers found.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setTextSize(14);
            empty.setPadding(dp(16), dp(18), dp(16), dp(18));
            container.addView(empty);
            return;
        }

        final android.widget.RadioButton[] selectedRadio = new android.widget.RadioButton[1];
        for (int i = 0; i < customers.size(); i++) {
            DatabaseHelper.SavedCustomer customer = customers.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(12), dp(8), dp(12));
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackgroundResource(android.R.drawable.list_selector_background);

            ImageView rowAvatar = new ImageView(this);
            LinearLayout.LayoutParams rowAvatarParams = new LinearLayout.LayoutParams(dp(36), dp(36));
            rowAvatarParams.setMarginEnd(dp(12));
            rowAvatar.setLayoutParams(rowAvatarParams);
            rowAvatar.setImageResource(R.drawable.ic_user);
            rowAvatar.setBackgroundResource(R.drawable.bg_icon_badge);
            rowAvatar.setPadding(dp(8), dp(8), dp(8), dp(8));
            rowAvatar.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_secondary)));
            rowAvatar.setContentDescription("Customer photo");
            CustomerAvatars.bind(this, rowAvatar, customer.getPhone(), customer.getPhotoPath(), dp(72), false, null);
            row.addView(rowAvatar);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView name = new TextView(this);
            name.setText(customer.getName() == null ? "Unnamed customer" : customer.getName());
            name.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            name.setTextSize(15);
            name.setTypeface(null, Typeface.BOLD);

            TextView phone = new TextView(this);
            phone.setText(customer.getPhone() == null ? "" : customer.getPhone());
            phone.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            phone.setTextSize(13);
            phone.setPadding(0, dp(3), 0, 0);
            info.addView(name);
            info.addView(phone);

            android.widget.RadioButton radio = new android.widget.RadioButton(this);
            radio.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_blue)));
            row.addView(info);
            row.addView(radio);

            View.OnClickListener select = v -> {
                if (selectedRadio[0] != null) selectedRadio[0].setChecked(false);
                radio.setChecked(true);
                selectedRadio[0] = radio;

                if (etCustomerName != null) etCustomerName.setText(customer.getName());
                if (etPhone != null) etPhone.setText(customer.getPhone());
                if (etEmail != null) etEmail.setText(customer.getEmail());
                selectedCustomerAddress = customer.getAddress() == null ? "" : customer.getAddress().trim();
                pinOwnerPhoneDigits = CustomerDirectory.digits(customer.getPhone());
                selectedCustomerLatitude = customer.getLatitude();
                selectedCustomerLongitude = customer.getLongitude();
                if (etCustomerAddress != null) etCustomerAddress.setText(selectedCustomerAddress);

                LinearLayout form = findViewById(R.id.llNewCustomerForm);
                if (form != null) form.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Selected customer: " + customer.getName(), Toast.LENGTH_SHORT).show();
            };
            row.setOnClickListener(select);
            radio.setOnClickListener(select);
            container.addView(row);

            if (i < customers.size() - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
                divider.setBackgroundColor(0x1A64748B);
                container.addView(divider);
            }
        }
    }

    private void clearRadioSelections(android.view.ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof android.widget.RadioButton) {
                ((android.widget.RadioButton) child).setChecked(false);
            } else if (child instanceof android.view.ViewGroup) {
                clearRadioSelections((android.view.ViewGroup) child);
            }
        }
    }

    private void showSavedCustomers() {
        List<DatabaseHelper.SavedCustomer> allCustomers = CustomerDirectory.list(dbHelper, serverBookings, serverCustomers, "");
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
                    List<DatabaseHelper.SavedCustomer> filtered = CustomerDirectory.list(dbHelper, serverBookings, serverCustomers, query);
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
            avatar.setContentDescription("Customer photo");
            CustomerAvatars.bind(this, avatar, customer.getPhone(), customer.getPhotoPath(), dp(88), false, null);

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
                selectedCustomerAddress = customer.getAddress() != null ? customer.getAddress().trim() : "";
                pinOwnerPhoneDigits = CustomerDirectory.digits(customer.getPhone());
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
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (phone.isEmpty() || cleanPhone.length() < 7) {
            etPhone.setError("Valid phone number is required"); etPhone.requestFocus(); return;
        }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email address is required"); etEmail.requestFocus(); return;
        }
        // A new customer is saved on the server with a photo (Add customer); one already there is only
        // remembered on this phone.
        if (!CustomerDirectory.onServer(serverCustomers, name, phone)) {
            openAddCustomer(name, phone, email, "A new customer needs a photo. Take or choose one, check the "
                    + "location, then save.");
            return;
        }
        if (dbHelper.saveCustomer(name, phone, email, address, selectedCustomerLatitude, selectedCustomerLongitude)) {
            selectedCustomerAddress = address;
            Toast.makeText(this, "Customer saved to directory.", Toast.LENGTH_SHORT).show();
            if (directoryOnly) finish();
        } else {
            Toast.makeText(this, "Unable to save customer. Check the required details.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_CONTINUE_AFTER_ADD, continueAfterAdd);
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
