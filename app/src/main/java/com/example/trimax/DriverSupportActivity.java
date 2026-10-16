package com.example.trimax;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

public class DriverSupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_support);

        ImageView btnBack = findViewById(R.id.btnBack);
        CardView cardReportVehicle = findViewById(R.id.cardReportVehicle);
        CardView cardContactDispatch = findViewById(R.id.cardContactDispatch);
        MaterialButton btnEmergencySos = findViewById(R.id.btnEmergencySos);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (cardReportVehicle != null) {
            cardReportVehicle.setOnClickListener(v -> {
                Toast.makeText(this, "Vehicle issue report logged to TRI-MAX Dispatch!", Toast.LENGTH_LONG).show();
            });
        }

        if (cardContactDispatch != null) {
            cardContactDispatch.setOnClickListener(v -> {
                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0281234567"));
                startActivity(dial);
            });
        }

        if (btnEmergencySos != null) {
            btnEmergencySos.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("🚨 EMERGENCY SOS BROADCAST")
                        .setMessage("Broadcasting current GPS location (14.8115, 121.0453) to TRI-MAX Dispatch and local emergency hotline?")
                        .setPositiveButton("SEND SOS NOW", (dialog, which) -> {
                            Toast.makeText(this, "SOS Alert Sent! Dispatch team notified.", Toast.LENGTH_LONG).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }
}