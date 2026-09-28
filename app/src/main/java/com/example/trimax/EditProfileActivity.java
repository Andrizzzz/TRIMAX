package com.example.trimax;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class EditProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        ImageView btnBack = findViewById(R.id.btnBack);
        MaterialButton btnSaveChanges = findViewById(R.id.btnSaveChanges);

        btnBack.setOnClickListener(v -> finish());

        btnSaveChanges.setOnClickListener(v -> {
            Toast.makeText(this, "Profile changes saved successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}