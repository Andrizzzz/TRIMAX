package com.example.trimax;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

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

        btnBack.setOnClickListener(v -> finish());

        // Setup Spinner options
        String[] services = {"Standard Cargo Express", "Fragile Package Handler", "Bulk Commercial Cargo", "VIP Priority Delivery"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, services);
        spinnerService.setAdapter(adapter);

        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                tvQuantity.setText(String.valueOf(quantity));
            }
        });

        btnPlus.setOnClickListener(v -> {
            quantity++;
            tvQuantity.setText(String.valueOf(quantity));
        });

        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());

        btnContinue.setOnClickListener(v -> {
            String service = spinnerService.getSelectedItem() != null ? spinnerService.getSelectedItem().toString() : "Standard Cargo";
            String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

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

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
            btnSelectDate.setText(selectedDate);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            btnSelectTime.setText(selectedTime);
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false);
        dialog.show();
    }
}