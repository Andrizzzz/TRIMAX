package com.example.trimax;

import android.content.ActivityNotFoundException;
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

    // Views
    private TextView tvHeaderTitle, tvStatusBadge, tvSeqBadge;
    private TextView tvCustomerName, tvPickupAddress, tvDestinationAddress, tvPackageDetails, tvSpecialInstructions;
    private TextView tvOriginalAmount, tvDepositSummary, tvAdjustmentsSummary, tvFinalTotalAmount;

    private MaterialButton btnCallCustomer, btnMessageCustomer;
    private MaterialButton btnPrimaryAction, btnNavigateMap, btnReportProblem, btnManageDeposit, btnReportDamage;

    private LinearLayout llMainContent;
    private LinearLayout llErrorState;
    private TextView tvErrorTitle, tvErrorMessage;
    private MaterialButton btnErrorBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_delivery_details);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);
        locationHelper = new LocationHelper(this);

        deliveryCode = getIntent().getStringExtra("DELIVERY_ID");
        if (TextUtils.isEmpty(deliveryCode)) {
            deliveryCode = getIntent().getStringExtra("BOOKING_ID");
        }
        if (deliveryCode != null) {
            deliveryCode = deliveryCode.trim();
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvSeqBadge = findViewById(R.id.tvSeqBadge);

        tvPackageDetails = findViewById(R.id.tvPackageDetails);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        btnCallCustomer = findViewById(R.id.btnCallCustomer);
        btnMessageCustomer = findViewById(R.id.btnMessageCustomer);

        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvDestinationAddress = findViewById(R.id.tvDestinationAddress);
        btnNavigateMap = findViewById(R.id.btnNavigateMap);

        tvSpecialInstructions = findViewById(R.id.tvSpecialInstructions);

        tvOriginalAmount = findViewById(R.id.tvOriginalAmount);
        tvDepositSummary = findViewById(R.id.tvDepositSummary);
        tvAdjustmentsSummary = findViewById(R.id.tvAdjustmentsSummary);
        tvFinalTotalAmount = findViewById(R.id.tvFinalTotalAmount);

        btnPrimaryAction = findViewById(R.id.btnPrimaryAction);
        btnReportProblem = findViewById(R.id.btnReportProblem);
        btnManageDeposit = findViewById(R.id.btnManageDeposit);
        btnReportDamage = findViewById(R.id.btnReportDamage);

        llMainContent = findViewById(R.id.llMainContent);
        llErrorState = findViewById(R.id.llErrorState);
        tvErrorTitle = findViewById(R.id.tvErrorTitle);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnErrorBack = findViewById(R.id.btnErrorBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        if (btnErrorBack != null) {
            btnErrorBack.setOnClickListener(v -> handleBackNavigation());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        if (btnCallCustomer != null) {
            btnCallCustomer.setOnClickListener(v -> {
                String phone = currentDelivery != null ? currentDelivery.getCustomerPhone() : "";
                if (!TextUtils.isEmpty(phone)) {
                    try {
                        Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                        startActivity(dial);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, "Dialer app not available on device", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "No phone number available for customer", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnMessageCustomer != null) {
            btnMessageCustomer.setOnClickListener(v -> {
                String phone = currentDelivery != null ? currentDelivery.getCustomerPhone() : "";
                if (!TextUtils.isEmpty(phone)) {
                    try {
                        Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phone));
                        sms.putExtra("sms_body", "Hello! This is your TRI-MAX driver regarding your delivery.");
                        startActivity(sms);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, "Messaging app not available on device", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "No phone number available for customer", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnNavigateMap != null) {
            btnNavigateMap.setOnClickListener(v -> {
                if (currentDelivery == null) return;
                Intent mapIntent = new Intent(this, DriverMapActivity.class);
                mapIntent.putExtra("DELIVERY_ID", currentDelivery.getBookingId());
                mapIntent.putExtra("BOOKING_ID", currentDelivery.getBookingId());
                mapIntent.putExtra("DEST_ADDRESS", currentDelivery.getDestination());
                mapIntent.putExtra("DEST_LAT", currentDelivery.getDestinationLatitude());
                mapIntent.putExtra("DEST_LNG", currentDelivery.getDestinationLongitude());
                mapIntent.putExtra("CUSTOMER_NAME", currentDelivery.getCustomerName());
                try {
                    startActivity(mapIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open live map navigation", Toast.LENGTH_SHORT).show();
                }
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
        // The delivery is loaded in onResume, which always follows onCreate.
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDeliveryData();
    }

    /** The delivery as the TRIMAX server has it; only deliveries assigned to the signed-in driver load. */
    private BookingRecord currentRecord;
    private boolean updating;

    private int bookingNumber() {
        try {
            return Integer.parseInt(deliveryCode != null ? deliveryCode.replace("#", "").trim() : "");
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void loadDeliveryData() {
        int id = bookingNumber();
        if (id <= 0) {
            showErrorState("Delivery Not Found", "Delivery " + (deliveryCode != null ? deliveryCode : "")
                    + " is not a TRIMAX server booking. Open it again from Deliveries.");
            return;
        }
        TrimaxApi.get(this).getDriverDelivery(id, new TrimaxApi.Callback<BookingRecord>() {
            @Override
            public void onSuccess(BookingRecord record) {
                if (SessionGuard.isGone(DriverDeliveryDetailsActivity.this)) return;
                showRecord(record);
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(DriverDeliveryDetailsActivity.this)) return;
                if (SessionGuard.handle(DriverDeliveryDetailsActivity.this, error)) return;
                if (currentRecord == null || error.getKind() == ApiException.Kind.NOT_FOUND) {
                    // NOT_FOUND also covers "reassigned to another driver": stop showing it.
                    showErrorState(error.getKind() == ApiException.Kind.NOT_FOUND ? "Delivery Not Available" : "Could Not Load Delivery",
                            error.getMessage());
                } else {
                    Toast.makeText(DriverDeliveryDetailsActivity.this, "Could not refresh: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showRecord(BookingRecord record) {
        currentRecord = record;
        currentDelivery = record.toDelivery(true);
        hideErrorState();

        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText(String.format("Booking #%s", currentDelivery.getBookingId()));
        }

        if (tvSeqBadge != null) {
            tvSeqBadge.setText(String.format(Locale.getDefault(), "Seq #%d", currentDelivery.getSequenceNumber()));
        }

        if (tvCustomerName != null) {
            tvCustomerName.setText(TextUtils.isEmpty(currentDelivery.getCustomerName()) ? "Customer" : currentDelivery.getCustomerName());
        }

        if (tvPickupAddress != null) {
            String pickup = currentDelivery.getPickupLocation();
            tvPickupAddress.setText(TextUtils.isEmpty(pickup) ? "Not specified" : pickup);
        }

        if (tvDestinationAddress != null) {
            String dest = currentDelivery.getDestination();
            tvDestinationAddress.setText(TextUtils.isEmpty(dest) ? "Not specified" : dest);
        }

        if (tvPackageDetails != null) {
            String details = currentDelivery.getPackageDetails();
            tvPackageDetails.setText(TextUtils.isEmpty(details) ? "Package details unavailable" : details);
        }

        if (tvSpecialInstructions != null) {
            String instructions = currentDelivery.getSpecialInstructions();
            tvSpecialInstructions.setText(TextUtils.isEmpty(instructions) ? "No special instructions provided." : instructions);
        }

        if (tvOriginalAmount != null) {
            tvOriginalAmount.setText(String.format(Locale.US, "₱ %,.2f", currentDelivery.getOriginalAmount()));
        }

        if (tvDepositSummary != null) {
            tvDepositSummary.setText(String.format(Locale.US, "₱ %,.2f (%s)", currentDelivery.getDepositAmount(), TextUtils.isEmpty(currentDelivery.getDepositStatus()) ? "N/A" : currentDelivery.getDepositStatus()));
        }

        List<OrderAdjustment> adjustments = dbHelper.getOrderAdjustments(currentDelivery.getBookingId());
        double totalAdj = 0;
        for (OrderAdjustment adj : adjustments) {
            String adjustmentType = adj.getType() != null ? adj.getType().toUpperCase(Locale.US) : "";
            if ("ADDITIONAL_CHARGE".equals(adjustmentType) || "DAMAGE_CHARGE".equals(adjustmentType)) {
                totalAdj += adj.getAmount();
            } else if ("REFUND".equals(adjustmentType)) {
                totalAdj -= adj.getAmount();
            } else if ("DEDUCTION".equals(adjustmentType) || "DEPOSIT".equals(adjustmentType)) {
                totalAdj -= adj.getAmount();
            }
        }

        if (tvAdjustmentsSummary != null) {
            tvAdjustmentsSummary.setText(String.format(Locale.US, "%s₱ %,.2f", totalAdj >= 0 ? "+" : "", totalAdj));
        }

        if (tvFinalTotalAmount != null) {
            tvFinalTotalAmount.setText(String.format(Locale.US, "₱ %,.2f", currentDelivery.getCurrentTotal()));
        }

        updateStatusAndButtons();
    }

    private void updateStatusAndButtons() {
        if (tvStatusBadge == null || btnPrimaryAction == null || currentRecord == null) return;

        String upperStatus = currentDelivery.getStatus() != null ? currentDelivery.getStatus().toUpperCase(Locale.US) : "ASSIGNED";
        tvStatusBadge.setText(DeliveryProgress.PENDING.equals(currentRecord.status) ? upperStatus + " \u2022 AWAITING ADMIN" : upperStatus);
        if ("ASSIGNED".equals(upperStatus)) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
            tvStatusBadge.setTextColor(0xFF2563EB);
        } else if ("DELIVERED".equals(upperStatus) || "COMPLETED".equals(upperStatus)) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_completed);
            tvStatusBadge.setTextColor(0xFF2563EB);
        } else if ("FAILED".equals(upperStatus) || "CANCELLED".equals(upperStatus)) {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_cancelled);
            tvStatusBadge.setTextColor(0xFFEF4444);
        } else {
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvStatusBadge.setTextColor(0xFF059669);
        }

        DeliveryProgress.NextStep step = DeliveryProgress.nextStep(currentRecord.status, currentRecord.deliveryStatus);
        btnPrimaryAction.setText(step.label);
        btnPrimaryAction.setEnabled(step.enabled && !updating);
        if (step.opensProof) {
            btnPrimaryAction.setOnClickListener(v -> {
                Intent proofIntent = new Intent(this, DriverProofActivity.class);
                proofIntent.putExtra("DELIVERY_ID", currentDelivery.getBookingId());
                try {
                    startActivity(proofIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open proof of delivery", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (step.target != null) {
            btnPrimaryAction.setOnClickListener(v -> advanceStatus(step.target, null, null));
        } else {
            btnPrimaryAction.setOnClickListener(null);
        }
    }

    /**
     * Sends one step to the server together with the status this screen is showing (Start Delivery:
     * ASSIGNED -> STARTED; "mark failed": STARTED -> FAILED). The screen then shows what the server saved;
     * nothing is assumed. While a step is being sent the button stays disabled, and the server answers an
     * exact repeat with the saved booking, so a double tap or a retry changes the status only once. If
     * someone else changed the booking meanwhile (e.g. the administrator cancelled it), the server refuses
     * and the screen reloads.
     */
    private void advanceStatus(String target, String note, Runnable onSaved) {
        if (currentRecord == null || updating) return;
        String expected = currentRecord.deliveryStatus != null ? currentRecord.deliveryStatus : DeliveryProgress.ASSIGNED;
        updating = true;
        btnPrimaryAction.setEnabled(false);
        CharSequence label = btnPrimaryAction.getText();
        btnPrimaryAction.setText("UPDATING...");
        Double lat = locationHelper.hasValidLocationFix() ? locationHelper.getLatitude() : null;
        Double lng = locationHelper.hasValidLocationFix() ? locationHelper.getLongitude() : null;
        TrimaxApi.get(this).updateDeliveryStatus(currentRecord.bookingId, expected, target, lat, lng, note, null,
                new TrimaxApi.Callback<BookingRecord>() {
                    @Override
                    public void onSuccess(BookingRecord saved) {
                        updating = false;
                        if (SessionGuard.isGone(DriverDeliveryDetailsActivity.this)) return;
                        dbHelper.logLocation(session.getUsername(), saved.idText(), locationHelper.getLatitude(),
                                locationHelper.getLongitude(), locationHelper.getAccuracy(), 0f, 0f);
                        Toast.makeText(DriverDeliveryDetailsActivity.this, "Saved: "
                                + DeliveryProgress.driverDisplayStatus(saved.status, saved.deliveryStatus), Toast.LENGTH_SHORT).show();
                        showRecord(saved);
                        if (onSaved != null) onSaved.run();
                    }

                    @Override
                    public void onError(ApiException error) {
                        updating = false;
                        if (SessionGuard.isGone(DriverDeliveryDetailsActivity.this)) return;
                        if (SessionGuard.handle(DriverDeliveryDetailsActivity.this, error)) return;
                        btnPrimaryAction.setText(label);
                        updateStatusAndButtons();
                        Toast.makeText(DriverDeliveryDetailsActivity.this, error.getKind() == ApiException.Kind.INVALID
                                ? DeliveryProgress.refusalMessage(error.getMessage()) : error.getMessage(), Toast.LENGTH_LONG).show();
                        if (error.getKind() == ApiException.Kind.CONFLICT || error.getKind() == ApiException.Kind.NOT_FOUND
                                || error.getKind() == ApiException.Kind.INVALID || error.getKind() == ApiException.Kind.TIMEOUT) {
                            loadDeliveryData();   // show what the server has now (a timed-out step may have been saved)
                        }
                    }
                });
    }

    private void showDepositDialog() {
        if (currentDelivery == null) return;
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
        etReason.setSingleLine(false);
        etReason.setMinLines(2);
        etReason.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        etReason.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams reasonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        reasonParams.topMargin = 12;
        etReason.setLayoutParams(reasonParams);
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

            boolean updated = dbHelper.updateDepositStatus(currentDelivery.getBookingId(), session.getUsername(), amt, chosenStatus, reason);
            if (updated) {
                // Deposits are not on the TRIMAX server yet: say so instead of implying the office was told.
                Toast.makeText(this, "Deposit noted as " + chosenStatus + " on this phone only. Tell the office.",
                        Toast.LENGTH_LONG).show();
                loadDeliveryData();
            } else {
                Toast.makeText(this, "Failed to update deposit", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showReportDamageDialog() {
        if (currentDelivery == null) return;
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

            boolean saved = dbHelper.addOrderAdjustment(currentDelivery.getBookingId(), session.getUsername(), "DAMAGE_CHARGE", chargeAmt, damageType + ": " + item, desc);
            if (saved) {
                // Damage charges are not on the TRIMAX server yet: the booking total there is unchanged.
                Toast.makeText(this, "Damage report saved on this phone only (₱" + chargeAmt
                        + " not charged). Call the office.", Toast.LENGTH_LONG).show();
                loadDeliveryData();
            } else {
                Toast.makeText(this, "Failed to submit damage report", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showReportProblemDialog() {
        if (currentDelivery == null) return;
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

            boolean saved = dbHelper.saveProblemReport(currentDelivery.getBookingId(), session.getUsername(), selectedType, desc, locationHelper.getLatitude(), locationHelper.getLongitude());
            if (saved) {
                Toast.makeText(this, "Problem report saved on this phone. Call the office if it needs action now.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to submit problem report", Toast.LENGTH_SHORT).show();
            }
        });
        // A delivery that cannot be finished is reported to the server; the administrator then decides
        // whether to cancel the booking (which returns the stock) from the desktop.
        if (currentRecord != null && DeliveryProgress.isUnderWay(currentRecord.deliveryStatus)) {
            builder.setNeutralButton("Mark delivery failed", (dialog, which) -> {
                String desc = etDescription.getText().toString().trim();
                String reason = problemTypes[selectedIndex[0]] + (desc.isEmpty() ? "" : ": " + desc);
                new AlertDialog.Builder(this)
                        .setTitle("Mark delivery failed?")
                        .setMessage("Booking #" + currentRecord.bookingId + " will be reported as FAILED (" + reason
                                + "). You cannot continue it afterwards; the administrator will review it.")
                        .setPositiveButton("Mark failed", (d2, w2) -> advanceStatus(DeliveryProgress.FAILED,
                                reason.length() > 480 ? reason.substring(0, 480) : reason, null))
                        .setNegativeButton("Back", null)
                        .show();
            });
        }

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showErrorState(String title, String message) {
        if (llMainContent != null) llMainContent.setVisibility(View.GONE);
        if (llErrorState != null) llErrorState.setVisibility(View.VISIBLE);
        if (tvErrorTitle != null) tvErrorTitle.setText(title);
        if (tvErrorMessage != null) tvErrorMessage.setText(message);
        if (tvHeaderTitle != null) tvHeaderTitle.setText("Delivery Error");
        if (tvStatusBadge != null) tvStatusBadge.setVisibility(View.GONE);
        if (tvSeqBadge != null) tvSeqBadge.setVisibility(View.GONE);
    }

    private void hideErrorState() {
        if (llMainContent != null) llMainContent.setVisibility(View.VISIBLE);
        if (llErrorState != null) llErrorState.setVisibility(View.GONE);
        if (tvStatusBadge != null) tvStatusBadge.setVisibility(View.VISIBLE);
        if (tvSeqBadge != null) tvSeqBadge.setVisibility(View.VISIBLE);
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
