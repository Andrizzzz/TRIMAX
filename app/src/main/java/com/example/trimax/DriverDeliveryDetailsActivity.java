package com.example.trimax;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class DriverDeliveryDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;
    private LocationHelper locationHelper;

    private String deliveryCode;
    private Delivery currentDelivery;

    private TextView tvHeaderTitle, tvStatusBadge, tvSeqBadge;
    private TextView tvCustomerName, tvPickupAddress, tvDestinationAddress, tvPackageDetails, tvSpecialInstructions;
    private TextView tvOriginalAmount, tvDepositSummary, tvAdjustmentsSummary, tvFinalTotalAmount;
    private MaterialButton btnPrimaryAction, btnNavigateMap, btnReportProblem, btnManageDeposit, btnReportDamage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_delivery_details);

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

        deliveryCode = getIntent().getStringExtra("DELIVERY_ID");
        if (deliveryCode == null || deliveryCode.isEmpty()) {
            deliveryCode = "TRX-1024";
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvSeqBadge = findViewById(R.id.tvSeqBadge);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvDestinationAddress = findViewById(R.id.tvDestinationAddress);
        tvPackageDetails = findViewById(R.id.tvPackageDetails);
        tvSpecialInstructions = findViewById(R.id.tvSpecialInstructions);

        tvOriginalAmount = findViewById(R.id.tvOriginalAmount);
        tvDepositSummary = findViewById(R.id.tvDepositSummary);
        tvAdjustmentsSummary = findViewById(R.id.tvAdjustmentsSummary);
        tvFinalTotalAmount = findViewById(R.id.tvFinalTotalAmount);

        MaterialButton btnCallCustomer = findViewById(R.id.btnCallCustomer);
        MaterialButton btnMessageCustomer = findViewById(R.id.btnMessageCustomer);

        btnPrimaryAction = findViewById(R.id.btnPrimaryAction);
        btnNavigateMap = findViewById(R.id.btnNavigateMap);
        btnReportProblem = findViewById(R.id.btnReportProblem);
        btnManageDeposit = findViewById(R.id.btnManageDeposit);
        btnReportDamage = findViewById(R.id.btnReportDamage);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        if (btnCallCustomer != null) {
            btnCallCustomer.setOnClickListener(v -> {
                String phone = currentDelivery != null ? currentDelivery.getCustomerPhone() : "09171234567";
                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                startActivity(dial);
            });
        }

        if (btnMessageCustomer != null) {
            btnMessageCustomer.setOnClickListener(v -> {
                String phone = currentDelivery != null ? currentDelivery.getCustomerPhone() : "09171234567";
                Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phone));
                sms.putExtra("sms_body", "Hello! This is your TRI-MAX driver regarding your delivery.");
                startActivity(sms);
            });
        }

        if (btnNavigateMap != null) {
            btnNavigateMap.setOnClickListener(v -> {
                Intent mapIntent = new Intent(this, DriverMapActivity.class);
                mapIntent.putExtra("DELIVERY_ID", deliveryCode);
                startActivity(mapIntent);
            });
        }

        if (btnReportProblem != null) {
            btnReportProblem.setOnClickListener(v -> showReportProblemDialog());
        }

        if (btnManageDeposit != null) {
            btnManageDeposit.setOnClickListener(v -> showDepositDialog());
        }

        if (btnReportDamage != null) {
            btnReportDamage.setOnClickListener(v -> showReportDamageDialog());
        }

        loadDeliveryData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDeliveryData();
    }

    private void loadDeliveryData() {
        currentDelivery = dbHelper.getDeliveryByCode(deliveryCode);
        if (currentDelivery == null) return;

        tvHeaderTitle.setText(String.format("Booking #%s", currentDelivery.getBookingId()));
        tvSeqBadge.setText(String.format(Locale.getDefault(), "Seq #%d", currentDelivery.getSequenceNumber()));
        tvCustomerName.setText(currentDelivery.getCustomerName());
        tvPickupAddress.setText(currentDelivery.getPickupLocation());
        tvDestinationAddress.setText(currentDelivery.getDestination());
        String packageName = currentDelivery.getPackageDetails();
        if (packageName == null || packageName.trim().isEmpty()) packageName = "Delivery Package";
        String scheduled = currentDelivery.getScheduledTime();
        tvPackageDetails.setText("📦 " + packageName + (scheduled != null && !scheduled.trim().isEmpty() ? " • " + scheduled : ""));

        String instructions = currentDelivery.getSpecialInstructions();
        if (TextUtils.isEmpty(instructions)) {
            instructions = "Standard delivery procedures apply.";
        }
        tvSpecialInstructions.setText(instructions);

        // Financial & Deposit breakdown
        tvOriginalAmount.setText(String.format(Locale.getDefault(), "₱ %.2f", currentDelivery.getOriginalAmount()));
        tvDepositSummary.setText(String.format(Locale.getDefault(), "₱ %.2f (%s)", currentDelivery.getDepositAmount(), currentDelivery.getDepositStatus()));

        List<OrderAdjustment> adjustments = dbHelper.getOrderAdjustments(deliveryCode);
        double totalAdj = 0;
        for (OrderAdjustment adj : adjustments) {
            if ("ADDITIONAL_CHARGE".equalsIgnoreCase(adj.getType()) || "DAMAGE_CHARGE".equalsIgnoreCase(adj.getType())) {
                totalAdj += adj.getAmount();
            } else {
                totalAdj -= adj.getAmount();
            }
        }
        tvAdjustmentsSummary.setText(String.format(Locale.getDefault(), "%s₱ %.2f", totalAdj >= 0 ? "+" : "", totalAdj));

        // Total
        tvFinalTotalAmount.setText(String.format(Locale.getDefault(), "₱ %.2f", currentDelivery.getCurrentTotal()));

        updateStatusAndButtons(currentDelivery.getStatus());
    }

    private void updateStatusAndButtons(String status) {
        tvStatusBadge.setText(status != null && !status.trim().isEmpty() ? status : "PENDING");
        btnPrimaryAction.setVisibility(View.VISIBLE);
        btnPrimaryAction.setEnabled(true);

        if ("ASSIGNED".equalsIgnoreCase(status)) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
            tvStatusBadge.setTextColor(0xFF2563EB);
            btnPrimaryAction.setText("START DELIVERY");
            btnPrimaryAction.setOnClickListener(v -> advanceStatus("STARTED", "Delivery Started! En route to pickup."));
        } else if ("STARTED".equalsIgnoreCase(status)) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvStatusBadge.setTextColor(0xFF059669);
            btnPrimaryAction.setText("GO TO PICKUP");
            btnPrimaryAction.setOnClickListener(v -> advanceStatus("GOING TO PICKUP", "En route to pickup hub location."));
        } else if ("GOING TO PICKUP".equalsIgnoreCase(status)) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvStatusBadge.setTextColor(0xFF059669);
            btnPrimaryAction.setText("ARRIVED AT PICKUP");
            btnPrimaryAction.setOnClickListener(v -> advanceStatus("ARRIVED AT PICKUP", "Arrived at pickup location!"));
        } else if ("ARRIVED AT PICKUP".equalsIgnoreCase(status)) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvStatusBadge.setTextColor(0xFF059669);
            btnPrimaryAction.setText("CONFIRM PICKUP");
            btnPrimaryAction.setOnClickListener(v -> advanceStatus("PICKED UP", "Package Picked Up successfully!"));
        } else if ("PICKED UP".equalsIgnoreCase(status)) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvStatusBadge.setTextColor(0xFF059669);
            btnPrimaryAction.setText("START TRANSPORT / ON THE WAY");
            btnPrimaryAction.setOnClickListener(v -> advanceStatus("ON THE WAY", "Delivery in transit to customer!"));
        } else if ("ON THE WAY".equalsIgnoreCase(status)) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvStatusBadge.setTextColor(0xFF059669);
            btnPrimaryAction.setText("ARRIVED AT DESTINATION");
            btnPrimaryAction.setOnClickListener(v -> advanceStatus("ARRIVED AT DESTINATION", "Arrived at destination location!"));
        } else if ("ARRIVED AT DESTINATION".equalsIgnoreCase(status)) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvStatusBadge.setTextColor(0xFF059669);
            btnPrimaryAction.setText("DROP-OFF PHOTO & PROOF OF DELIVERY");
            btnPrimaryAction.setOnClickListener(v -> {
                Intent proofIntent = new Intent(this, DriverProofActivity.class);
                proofIntent.putExtra("DELIVERY_ID", deliveryCode);
                startActivity(proofIntent);
            });
        } else if ("DELIVERED".equalsIgnoreCase(status)) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvStatusBadge.setTextColor(0xFF059669);
            btnPrimaryAction.setText("✓ DELIVERY COMPLETED");
            btnPrimaryAction.setEnabled(false);
        } else {
            tvStatusBadge.setText(status);
            btnPrimaryAction.setText("STATUS: " + status);
            btnPrimaryAction.setEnabled(false);
        }
    }

    private void advanceStatus(String newStatus, String toastMsg) {
        dbHelper.updateDeliveryStatus(deliveryCode, newStatus, locationHelper.getLatitude(), locationHelper.getLongitude());
        dbHelper.logLocation(session.getUsername(), deliveryCode, locationHelper.getLatitude(), locationHelper.getLongitude(), locationHelper.getAccuracy(), 0f, 0f);
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
        loadDeliveryData();
    }

    private void showDepositDialog() {
        String[] statuses = {"COLLECTED", "RETURNED", "FORFEITED", "PENDING"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Deposit (Current: ₱" + currentDelivery.getDepositAmount() + ")");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 24, 32, 24);

        final EditText etAmount = new EditText(this);
        etAmount.setHint("Deposit Amount (₱)");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etAmount.setText(String.valueOf(currentDelivery.getDepositAmount()));
        container.addView(etAmount);

        final EditText etReason = new EditText(this);
        etReason.setHint("Reason / Notes...");
        etReason.setPadding(0, 16, 0, 0);
        container.addView(etReason);

        builder.setView(container);
        builder.setSingleChoiceItems(statuses, 0, null);

        builder.setPositiveButton("Update Deposit", (dialog, which) -> {
            int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            String chosenStatus = statuses[selectedPosition >= 0 ? selectedPosition : 0];
            String amtStr = etAmount.getText().toString().trim();
            double amt = currentDelivery.getDepositAmount();
            try {
                if (!amtStr.isEmpty()) amt = Double.parseDouble(amtStr);
            } catch (Exception ignored) {}

            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) reason = "Deposit status updated to " + chosenStatus;

            boolean updated = dbHelper.updateDepositStatus(deliveryCode, session.getUsername(), amt, chosenStatus, reason);
            if (updated) {
                Toast.makeText(this, "Deposit updated to " + chosenStatus, Toast.LENGTH_SHORT).show();
                loadDeliveryData();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showReportDamageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report Damage / Broken Item");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);

        final EditText etItem = new EditText(this);
        etItem.setHint("Item Name (e.g. Glass Container)");
        layout.addView(etItem);

        String[] damageTypes = {"Broken", "Damaged", "Missing", "Other"};
        final Spinner spinnerType = new Spinner(this);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, damageTypes);
        spinnerType.setAdapter(adapter);
        layout.addView(spinnerType);

        final EditText etDesc = new EditText(this);
        etDesc.setHint("Damage Description...");
        layout.addView(etDesc);

        final EditText etCharge = new EditText(this);
        etCharge.setHint("Additional Damage Charge Amount (₱)");
        etCharge.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etCharge);

        builder.setView(layout);

        builder.setPositiveButton("Submit Damage Report", (dialog, which) -> {
            String item = etItem.getText().toString().trim();
            String damageType = damageTypes[spinnerType.getSelectedItemPosition()];
            String desc = etDesc.getText().toString().trim();
            String chargeStr = etCharge.getText().toString().trim();
            double chargeAmt = 0;
            try {
                if (!chargeStr.isEmpty()) chargeAmt = Double.parseDouble(chargeStr);
            } catch (Exception ignored) {}

            if (item.isEmpty()) item = "Package Item";
            if (desc.isEmpty()) desc = "Damage reported by driver";

            boolean saved = dbHelper.addOrderAdjustment(deliveryCode, session.getUsername(), "DAMAGE_CHARGE", chargeAmt, damageType + ": " + item, desc);
            if (saved) {
                Toast.makeText(this, "Damage report submitted. Additional charge ₱" + chargeAmt + " applied.", Toast.LENGTH_LONG).show();
                loadDeliveryData();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showReportProblemDialog() {
        String[] problemTypes = {
                "Vehicle problem",
                "Accident/emergency",
                "Cannot reach pickup location",
                "Incorrect pickup information",
                "Customer issue",
                "Package issue",
                "Other problem"
        };

        final int[] selectedIndex = {0};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report Problem to Admin");

        final EditText etDescription = new EditText(this);
        etDescription.setHint("Enter details for Admin review...");
        etDescription.setPadding(32, 24, 32, 24);

        builder.setSingleChoiceItems(problemTypes, 0, (dialog, which) -> selectedIndex[0] = which);
        builder.setView(etDescription);

        builder.setPositiveButton("Submit Report", (dialog, which) -> {
            String selectedType = problemTypes[selectedIndex[0]];
            String desc = etDescription.getText().toString().trim();

            if (desc.isEmpty()) {
                desc = "Problem reported: " + selectedType;
            }

            boolean saved = dbHelper.saveProblemReport(deliveryCode, session.getUsername(), selectedType, desc, locationHelper.getLatitude(), locationHelper.getLongitude());
            if (saved) {
                Toast.makeText(this, "Problem report sent to Admin! Admin will review and process.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to submit problem report", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void handleBackNavigation() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DriverDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
