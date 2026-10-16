package com.example.trimax;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class EditProfileActivity extends AppCompatActivity {

    private SessionManager session;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        session = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        MaterialButton btnSaveChanges = findViewById(R.id.btnSaveChanges);
        TextInputEditText etFullName = findViewById(R.id.etFullName);
        TextInputEditText etEmail = findViewById(R.id.etEmail);
        TextInputEditText etPhone = findViewById(R.id.etPhone);

        String currentName = session.getFullName();
        String currentEmail = session.getEmail();
        String currentPhone = session.getPhone();

        if (etFullName != null) etFullName.setText(currentName);
        if (etEmail != null) etEmail.setText(currentEmail);
        if (etPhone != null) etPhone.setText(currentPhone);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnSaveChanges != null) {
            btnSaveChanges.setOnClickListener(v -> {
                String newName = etFullName != null && etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
                String newEmail = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                String newPhone = etPhone != null && etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

                if (newName.isEmpty()) {
                    if (etFullName != null) {
                        etFullName.setError("Full name is required");
                        etFullName.requestFocus();
                    }
                } else if (newEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    if (etEmail != null) {
                        etEmail.setError("Valid email address is required");
                        etEmail.requestFocus();
                    }
                } else if (newPhone.isEmpty() || !Patterns.PHONE.matcher(newPhone).matches()) {
                    if (etPhone != null) {
                        etPhone.setError("Valid phone number is required");
                        etPhone.requestFocus();
                    }
                } else {
                    btnSaveChanges.setEnabled(false);

                    // Update SQLite Database and Session Memory
                    int userId = session.getUserId();
                    dbHelper.updateUserProfile(userId, newName, newEmail, newPhone);
                    session.updateProfile(newName, newEmail, newPhone);

                    Toast.makeText(this, "Profile changes saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }
}
