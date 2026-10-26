package com.example.trimax;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

        // Keep screen content clear of the status and navigation bars.
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);
        locationHelper = new LocationHelper(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        CardView cardReportVehicle = findViewById(R.id.cardReportVehicle);
        CardView cardContactDispatch = findViewById(R.id.cardContactDispatch);
        MaterialButton btnEmergencySos = findViewById(R.id.btnEmergencySos);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        if (cardReportVehicle != null) {
            cardReportVehicle.setOnClickListener(v -> showReportVehicleDialog());
        }

        if (cardContactDispatch != null) {
            cardContactDispatch.setOnClickListener(v -> {
                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + getString(R.string.office_phone)));
                startActivity(dial);
            });
        }

        if (btnEmergencySos != null) {
            btnEmergencySos.setOnClickListener(v -> {
                String where = locationHelper.hasValidLocationFix()
                        ? String.format(Locale.US, "your location (%.4f, %.4f)",
                                locationHelper.getLatitude(), locationHelper.getLongitude())
                        : "an alert (GPS location not available yet)";

                new AlertDialog.Builder(this)
                        .setTitle("🚨 EMERGENCY SOS")
                        .setMessage("Send " + where + " to the TRI-MAX office now? "
                                + "If you are in danger, also call the office or 911.")
                        .setPositiveButton("SEND SOS NOW", (dialog, which) ->
                                // Kept on this phone and sent to the admin as an SOS alert (retried when offline).
                                IssueReporter.send(this, true, null, "SOS",
                                        "Emergency SOS from the mobile app.", locationHelper))
                        .setNeutralButton("Call office", (dialog, which) -> startActivity(new Intent(Intent.ACTION_DIAL,
                                Uri.parse("tel:" + getString(R.string.office_phone)))))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // GPS for this screen, so an SOS or a vehicle report carries where the driver is.
        if (!locationHelper.isTrackingActive()) locationHelper.startLocationUpdates(null);
        IssueReporter.flush(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationHelper.stopLocationUpdates();
    }

    private void handleBackNavigation() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DriverDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
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

                    // Kept on this phone and sent to the admin (queued and retried when offline).
                    IssueReporter.send(this, false, null, "Vehicle breakdown", desc, locationHelper);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
