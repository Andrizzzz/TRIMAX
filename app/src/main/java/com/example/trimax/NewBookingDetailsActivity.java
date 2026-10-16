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
import android.widget.Toast;

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

        ImageView btnBack = findViewById(R.id.btnBack);
        spinnerService = findViewById(R.id.spinnerService);
        tvQuantity = findViewById(R.id.tvQuantity);
        MaterialButton btnMinus = findViewById(R.id.btnMinus);
        MaterialButton btnPlus = findViewById(R.id.btnPlus);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        etNotes = findViewById(R.id.etNotes);
        btnContinue = findViewById(R.id.btnContinue);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Setup Spinner options
        String[] services = {"Standard Cargo Express", "Fragile Package Handler", "Bulk Commercial Cargo", "VIP Priority Delivery"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, services);
        if (spinnerService != null) spinnerService.setAdapter(adapter);

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
                String service = (spinnerService != null && spinnerService.getSelectedItem() != null) ? spinnerService.getSelectedItem().toString() : "Standard Cargo";
                String notes = etNotes != null && etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

                btnContinue.setEnabled(false);

                Intent intent = new Intent(NewBookingDetailsActivity.this, NewBookingLocationActivity.class);
                intent.putExtra("CUSTOMER_NAME", customerName);
                intent.putExtra("CUSTOMER_PHONE", customerPhone);
                intent.putExtra("CUSTOMER_EMAIL", customerEmail);
                intent.putExtra("SERVICE", service);
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
}
