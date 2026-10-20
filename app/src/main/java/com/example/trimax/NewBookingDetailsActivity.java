package com.example.trimax;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

public class NewBookingDetailsActivity extends AppCompatActivity {

    private Spinner spinnerService;
    private TextView tvQuantity;
    private MaterialButton btnSelectDate, btnSelectTime, btnContinue;
    private TextInputEditText etNotes;

    private int quantity = 1;
    private String selectedDate = "";
    private String selectedTime = "";

    private String customerName, customerPhone, customerEmail;
    private String customerAddress;
    private double customerLatitude, customerLongitude;
    private String selectedPackageName = "";
    private double selectedPackagePrice = 99.00;

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

        customerName = getIntent().getStringExtra("CUSTOMER_NAME");
        customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        customerEmail = getIntent().getStringExtra("CUSTOMER_EMAIL");
        customerAddress = getIntent().getStringExtra("CUSTOMER_ADDRESS");
        customerLatitude = getIntent().getDoubleExtra("CUSTOMER_LAT", 0);
        customerLongitude = getIntent().getDoubleExtra("CUSTOMER_LNG", 0);
        selectedPackageName = getIntent().getStringExtra("SELECTED_PACKAGE");
        selectedPackagePrice = getIntent().getDoubleExtra("SELECTED_PRICE", 99.00);

        ImageView btnBack = findViewById(R.id.btnBack);
        spinnerService = findViewById(R.id.spinnerService);
        tvQuantity = findViewById(R.id.tvQuantity);
        MaterialButton btnMinus = findViewById(R.id.btnMinus);
        MaterialButton btnPlus = findViewById(R.id.btnPlus);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        etNotes = findViewById(R.id.etNotes);
        btnContinue = findViewById(R.id.btnContinue);

        if (btnBack != null) btnBack.setOnClickListener(v -> navigateBack());

        // Setup Spinner options (Strictly NO Standard Delivery)
        String[] services = {"Basic Delivery", "Express Delivery", "Bulk Delivery", "Fragile Delivery"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, services);
        if (spinnerService != null) {
            spinnerService.setAdapter(adapter);
            if (selectedPackageName != null) {
                for (int i = 0; i < services.length; i++) {
                    if (services[i].equalsIgnoreCase(selectedPackageName)) {
                        spinnerService.setSelection(i);
                        break;
                    }
                }
            }
        }

        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> {
                if (quantity > 1) {
                    quantity--;
                    if (tvQuantity != null) tvQuantity.setText(String.valueOf(quantity));
                }
            });
        }

        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                quantity++;
                if (tvQuantity != null) tvQuantity.setText(String.valueOf(quantity));
            });
        }

        if (btnSelectDate != null) btnSelectDate.setOnClickListener(v -> showDatePicker());
        if (btnSelectTime != null) btnSelectTime.setOnClickListener(v -> showTimePicker());

        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                String service = (spinnerService != null && spinnerService.getSelectedItem() != null) ? spinnerService.getSelectedItem().toString() : "Basic Delivery";
                String notes = etNotes != null && etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

                btnContinue.setEnabled(false);

                // Determine base price according to selected service if not set
                double price = selectedPackagePrice;
                if (service.contains("Express")) price = 249.00;
                else if (service.contains("Bulk")) price = 399.00;
                else if (service.contains("Fragile")) price = 299.00;
                else if (service.contains("Basic")) price = 99.00;

                Intent intent = new Intent(NewBookingDetailsActivity.this, NewBookingLocationActivity.class);
                intent.putExtra("CUSTOMER_NAME", customerName);
                intent.putExtra("CUSTOMER_PHONE", customerPhone);
                intent.putExtra("CUSTOMER_EMAIL", customerEmail);
                intent.putExtra("CUSTOMER_ADDRESS", customerAddress);
                intent.putExtra("CUSTOMER_LAT", customerLatitude);
                intent.putExtra("CUSTOMER_LNG", customerLongitude);
                intent.putExtra("SERVICE", service);
                intent.putExtra("SELECTED_PRICE", price);
                intent.putExtra("QUANTITY", quantity);
                intent.putExtra("DATE", selectedDate.isEmpty() ? "July 19, 2026" : selectedDate);
                intent.putExtra("TIME", selectedTime.isEmpty() ? "10:00 AM" : selectedTime);
                intent.putExtra("NOTES", notes);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (btnContinue != null) {
            btnContinue.setEnabled(true);
        }
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
            if (btnSelectDate != null) btnSelectDate.setText(selectedDate);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            if (btnSelectTime != null) btnSelectTime.setText(selectedTime);
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false);
        dialog.show();
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
