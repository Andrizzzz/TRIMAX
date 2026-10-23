package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AddCustomerActivity extends AppCompatActivity {
    private TextInputEditText etName, etPhone, etEmail;
    private TextView tvSelectedLocation;
    private String selectedAddress = "";
    private double selectedLatitude = 0, selectedLongitude = 0;
    private ActivityResultLauncher<Intent> locationPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_customer);

        View root = findViewById(R.id.addCustomerRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        etName = findViewById(R.id.etAddCustomerName);
        etPhone = findViewById(R.id.etAddCustomerPhone);
        etEmail = findViewById(R.id.etAddCustomerEmail);
        tvSelectedLocation = findViewById(R.id.tvAddCustomerLocation);

        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    Intent data = result.getData();
                    selectedAddress = data.getStringExtra("CUSTOMER_ADDRESS");
                    if (selectedAddress == null) selectedAddress = "";
                    selectedLatitude = data.getDoubleExtra("CUSTOMER_LAT", 0);
                    selectedLongitude = data.getDoubleExtra("CUSTOMER_LNG", 0);
                    if (tvSelectedLocation != null) {
                        tvSelectedLocation.setText(selectedAddress.isEmpty()
                                ? "No location selected" : selectedAddress);
                    }
                });

        View btnBack = findViewById(R.id.btnAddCustomerBack);
        MaterialButton btnChooseLocation = findViewById(R.id.btnChooseCustomerLocation);
        MaterialButton btnSave = findViewById(R.id.btnSaveNewCustomer);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnChooseLocation != null) btnChooseLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, NewBookingLocationActivity.class);
            intent.putExtra("CUSTOMER_LOCATION_PICKER_ONLY", true);
            intent.putExtra("CUSTOMER_NAME", textOf(etName));
            intent.putExtra("CUSTOMER_PHONE", textOf(etPhone));
            intent.putExtra("CUSTOMER_EMAIL", textOf(etEmail));
            intent.putExtra("CUSTOMER_ADDRESS", selectedAddress);
            intent.putExtra("CUSTOMER_LAT", selectedLatitude);
            intent.putExtra("CUSTOMER_LNG", selectedLongitude);
            locationPickerLauncher.launch(intent);
        });

        if (btnSave != null) btnSave.setOnClickListener(v -> saveCustomer());
    }

    private void saveCustomer() {
        String name = textOf(etName);
        String phone = textOf(etPhone);
        String email = textOf(etEmail);
        String cleanPhone = phone.replaceAll("[^0-9]", "");

        if (name.isEmpty()) {
            etName.setError("Customer name is required");
            etName.requestFocus();
            return;
        }
        if (phone.isEmpty() || cleanPhone.length() < 7) {
            etPhone.setError("Enter a valid phone number");
            etPhone.requestFocus();
            return;
        }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (selectedAddress.trim().isEmpty()
                || !Double.isFinite(selectedLatitude) || !Double.isFinite(selectedLongitude)
                || selectedLatitude < -90 || selectedLatitude > 90
                || selectedLongitude < -180 || selectedLongitude > 180
                || (selectedLatitude == 0 && selectedLongitude == 0)) {
            Toast.makeText(this, "Please select the customer's location first.", Toast.LENGTH_LONG).show();
            return;
        }

        MaterialButton btnSave = findViewById(R.id.btnSaveNewCustomer);
        if (btnSave != null) btnSave.setEnabled(false);
        boolean saved = new DatabaseHelper(this).saveCustomer(
                name, phone, email, selectedAddress, selectedLatitude, selectedLongitude);
        if (!saved) {
            if (btnSave != null) btnSave.setEnabled(true);
            Toast.makeText(this, "Unable to save customer. Check the details and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent result = new Intent();
        result.putExtra("CUSTOMER_NAME", name);
        result.putExtra("CUSTOMER_PHONE", phone);
        result.putExtra("CUSTOMER_EMAIL", email);
        result.putExtra("CUSTOMER_ADDRESS", selectedAddress);
        result.putExtra("CUSTOMER_LAT", selectedLatitude);
        result.putExtra("CUSTOMER_LNG", selectedLongitude);
        setResult(RESULT_OK, result);
        Toast.makeText(this, "Customer saved.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String textOf(TextInputEditText field) {
        return field != null && field.getText() != null ? field.getText().toString().trim() : "";
    }
}
