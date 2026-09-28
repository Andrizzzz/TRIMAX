package com.example.trimax;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class ChangePasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        ImageView btnBack = findViewById(R.id.btnBack);
        MaterialButton btnChangePassword = findViewById(R.id.btnChangePassword);

        btnBack.setOnClickListener(v -> finish());

        btnChangePassword.setOnClickListener(v -> {
            Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}