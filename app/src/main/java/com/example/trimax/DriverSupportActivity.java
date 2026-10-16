package com.example.trimax;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class DriverSupportActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;
    private LocationHelper locationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_support);

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);
        locationHelper = new LocationHelper(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        CardView cardReportVehicle = findViewById(R.id.cardReportVehicle);
        CardView cardContactDispatch = findViewById(R.id.cardContactDispatch);
        MaterialButton btnEmergencySos = findViewById(R.id.btnEmergencySos);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (cardReportVehicle != null) {
            cardReportVehicle.setOnClickListener(v -> showReportVehicleDialog());
        }

        if (cardContactDispatch != null) {
            cardContactDispatch.setOnClickListener(v -> {
                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:0281234567"));
                startActivity(dial);
            });
        }

        if (btnEmergencySos != null) {
            btnEmergencySos.setOnClickListener(v -> {
                double lat = locationHelper.getLatitude();
                double lng = locationHelper.getLongitude();
                String gpsStr = String.format(Locale.US, "(%.4f, %.4f)", lat, lng);

                new AlertDialog.Builder(this)
                        .setTitle("🚨 EMERGENCY SOS BROADCAST")
                        .setMessage(String.format("Broadcasting current GPS location %s to TRI-MAX Dispatch and emergency hotline?", gpsStr))
                        .setPositiveButton("SEND SOS NOW", (dialog, which) -> {
                            boolean saved = dbHelper.saveProblemReport(
                                    "EMERGENCY_SOS",
                                    session.getUsername(),
                                    "EMERGENCY_SOS",
                                    "Emergency SOS alert broadcasted from mobile app.",
                                    lat,
                                    lng
                            );
                            if (saved) {
                                Toast.makeText(this, "🚨 SOS Alert Sent! Dispatch team notified.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "SOS Alert sent to Dispatch team.", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private void showReportVehicleDialog() {
        final EditText etDesc = new EditText(this);
        etDesc.setHint("Describe vehicle or mechanical breakdown...");
        etDesc.setPadding(32, 24, 32, 24);

        new AlertDialog.Builder(this)
                .setTitle("Report Vehicle Breakdown / Issue")
                .setView(etDesc)
                .setPositiveButton("Submit Report", (dialog, which) -> {
                    String desc = etDesc.getText().toString().trim();
                    if (desc.isEmpty()) desc = "Vehicle breakdown reported.";

                    boolean saved = dbHelper.saveProblemReport(
                            "VEHICLE_ISSUE",
                            session.getUsername(),
                            "VEHICLE_BREAKDOWN",
                            desc,
                            locationHelper.getLatitude(),
                            locationHelper.getLongitude()
                    );
                    if (saved) {
                        Toast.makeText(this, "Vehicle issue report logged to TRI-MAX Dispatch!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Report submitted successfully.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
