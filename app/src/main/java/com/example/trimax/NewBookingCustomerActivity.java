package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class NewBookingCustomerActivity extends AppCompatActivity {

    private TextInputEditText etCustomerName, etPhone, etEmail, etCustomerAddress;
    private DatabaseHelper dbHelper;
    private String selectedCustomerAddress = "";
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
        MaterialButton btnChooseCustomer = findViewById(R.id.btnChooseCustomer);
        MaterialButton btnAddCustomer = findViewById(R.id.btnAddCustomer);
        MaterialButton btnSaveCustomer = findViewById(R.id.btnSaveCustomer);
        MaterialButton btnNext = findViewById(R.id.btnNext);
        android.widget.TextView tvStepTitle = findViewById(R.id.tvStepTitle);
        if (directoryOnly) {
            if (tvStepTitle != null) tvStepTitle.setText("Add Customer");
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
                    dbHelper.saveCustomer(name, phone, email, selectedCustomerAddress);

                    Intent intent = new Intent(NewBookingCustomerActivity.this, NewBookingDetailsActivity.class);
                    intent.putExtra("CUSTOMER_NAME", name);
                    intent.putExtra("CUSTOMER_PHONE", phone);
                    intent.putExtra("CUSTOMER_EMAIL", email);
                    intent.putExtra("CUSTOMER_ADDRESS", selectedCustomerAddress);
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

    private void showSavedCustomers() {
        java.util.List<DatabaseHelper.SavedCustomer> customers = dbHelper.getSavedCustomers("");
        if (customers.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Saved Customers")
                    .setMessage("No saved customers yet. Enter the details and tap Save Customer to Directory.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        String[] labels = new String[customers.size()];
        for (int i = 0; i < customers.size(); i++) {
            DatabaseHelper.SavedCustomer customer = customers.get(i);
            labels[i] = customer.getName() + "  •  " + customer.getPhone()
                    + (customer.getAddress() == null || customer.getAddress().trim().isEmpty()
                    ? "" : "\n" + customer.getAddress());
        }
        new AlertDialog.Builder(this).setTitle("Select Saved Customer").setItems(labels, (dialog, which) -> {
            DatabaseHelper.SavedCustomer customer = customers.get(which);
            etCustomerName.setText(customer.getName());
            etPhone.setText(customer.getPhone());
            etEmail.setText(customer.getEmail());
            selectedCustomerAddress = customer.getAddress() != null ? customer.getAddress() : "";
            if (etCustomerAddress != null) etCustomerAddress.setText(selectedCustomerAddress);
            Toast.makeText(this, "Customer information loaded.", Toast.LENGTH_SHORT).show();
        }).setNegativeButton("Cancel", null).show();
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
        if (dbHelper.saveCustomer(name, phone, email, address)) {
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
