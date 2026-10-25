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
        // GPS for this screen: status changes and problem reports carry where the driver was.
        if (!locationHelper.isTrackingActive()) locationHelper.startLocationUpdates(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationHelper.stopLocationUpdates();
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
     * Sends one step to the server together with the status this screen is showing. The screen then shows
     * what the server saved; nothing is assumed. If someone else changed the booking meanwhile (e.g. the
     * administrator cancelled it), the server refuses and the screen reloads.
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
                        btnPrimaryAction.setEnabled(true);
                        Toast.makeText(DriverDeliveryDetailsActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                        if (error.getKind() == ApiException.Kind.CONFLICT || error.getKind() == ApiException.Kind.NOT_FOUND) {
                            loadDeliveryData();   // show what the server has now
                        }
                    }
                });
    }

    private void showDepositDialog() {
        if (currentDelivery == null) return;
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_manage_deposit, null);
        bottomSheetDialog.setContentView(dialogView);

        ImageView btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        android.widget.RadioGroup rgStatus = dialogView.findViewById(R.id.rgDepositStatus);
        com.google.android.material.textfield.TextInputEditText etAmount = dialogView.findViewById(R.id.etDepositAmount);
        MaterialButton btnUpdate = dialogView.findViewById(R.id.btnUpdateDeposit);

        if (btnClose != null) btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        if (etAmount != null) etAmount.setText(String.valueOf(currentDelivery.getDepositAmount()));

        String currentStatus = currentDelivery.getDepositStatus();
        if (rgStatus != null && currentStatus != null) {
            if ("COLLECTED".equalsIgnoreCase(currentStatus)) rgStatus.check(R.id.rbCollected);
            else if ("RETURNED".equalsIgnoreCase(currentStatus)) rgStatus.check(R.id.rbReturned);
            else if ("FORFEITED".equalsIgnoreCase(currentStatus)) rgStatus.check(R.id.rbForfeited);
            else rgStatus.check(R.id.rbPending);
        }

        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> {
                int checkedId = rgStatus != null ? rgStatus.getCheckedRadioButtonId() : -1;
                String chosenStatus = "PENDING";
                if (checkedId == R.id.rbCollected) chosenStatus = "COLLECTED";
                else if (checkedId == R.id.rbReturned) chosenStatus = "RETURNED";
                else if (checkedId == R.id.rbForfeited) chosenStatus = "FORFEITED";

                String amtStr = etAmount != null && etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
                double amt = currentDelivery.getDepositAmount();
                try {
                    if (!amtStr.isEmpty()) amt = Double.parseDouble(amtStr);
                } catch (Exception ignored) {}

                String reason = "Deposit status updated to " + chosenStatus;

                boolean updated = dbHelper.updateDepositStatus(currentDelivery.getBookingId(), session.getUsername(), amt, chosenStatus, reason);
                if (updated) {
                    Toast.makeText(this, "Deposit noted as " + chosenStatus + " on this phone only. Tell the office.",
                            Toast.LENGTH_LONG).show();
                    loadDeliveryData();
                    bottomSheetDialog.dismiss();
                } else {
                    Toast.makeText(this, "Failed to update deposit", Toast.LENGTH_SHORT).show();
                }
            });
        }
        bottomSheetDialog.show();
    }

    private void showReportDamageDialog() {
        if (currentDelivery == null) return;
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_report_damage, null);
        bottomSheetDialog.setContentView(dialogView);

        ImageView btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        com.google.android.material.textfield.TextInputEditText etAmount = dialogView.findViewById(R.id.etDamageAmount);
        com.google.android.material.textfield.TextInputEditText etDesc = dialogView.findViewById(R.id.etDamageDescription);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btnSubmitDamage);

        if (btnClose != null) btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String chargeStr = etAmount != null && etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
                String desc = etDesc != null && etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                double chargeAmt = 0;
                try {
                    if (!chargeStr.isEmpty()) chargeAmt = Double.parseDouble(chargeStr);
                } catch (Exception ignored) {}

                if (desc.isEmpty()) desc = "Damage reported by driver";

                boolean saved = dbHelper.addOrderAdjustment(currentDelivery.getBookingId(), session.getUsername(), "DAMAGE_CHARGE", chargeAmt, "Damaged Item", desc);
                if (saved) {
                    Toast.makeText(this, "Damage report saved on this phone only (₱" + chargeAmt
                            + " not charged). Call the office.", Toast.LENGTH_LONG).show();
                    loadDeliveryData();
                    bottomSheetDialog.dismiss();
                } else {
                    Toast.makeText(this, "Failed to submit damage report", Toast.LENGTH_SHORT).show();
                }
            });
        }
        bottomSheetDialog.show();
    }

    private void showReportProblemDialog() {
        if (currentDelivery == null) return;
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_report_problem, null);
        bottomSheetDialog.setContentView(dialogView);

        ImageView btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        android.widget.RadioGroup rg = dialogView.findViewById(R.id.rgProblemTypes);
        com.google.android.material.textfield.TextInputEditText etDesc = dialogView.findViewById(R.id.etProblemDescription);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btnSubmitReport);

        if (btnClose != null) btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                int checkedId = rg != null ? rg.getCheckedRadioButtonId() : -1;
                String selectedType = "Other problem";
                if (checkedId == R.id.rbVehicle) selectedType = "Vehicle problem";
                else if (checkedId == R.id.rbCustomer) selectedType = "Customer unreachable";
                else if (checkedId == R.id.rbAddress) selectedType = "Incorrect address";
                else if (checkedId == R.id.rbOther) selectedType = "Other problem";

                String details = etDesc != null && etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                String desc = "Reported: " + selectedType + (details.isEmpty() ? "" : " - " + details);

                // Kept on this phone and sent to the admin's Notifications (queued and retried when offline).
                // The real GPS fix is sent only when there is one; no stand-in location goes to the admin.
                IssueReporter.send(this, false, currentDelivery.getBookingId(), selectedType, desc, locationHelper);
                bottomSheetDialog.dismiss();
            });
        }

        // A started delivery that cannot be finished: end it as FAILED (the server needs a reason); the
        // admin is alerted and decides from the desktop whether to cancel the booking (returns the stock).
        MaterialButton btnMarkFailed = dialogView.findViewById(R.id.btnMarkFailed);
        boolean canFail = currentRecord != null && DeliveryProgress.isUnderWay(currentRecord.deliveryStatus)
                && !DeliveryProgress.CANCELLED.equals(currentRecord.status)
                && !DeliveryProgress.COMPLETED.equals(currentRecord.status);
        if (btnMarkFailed != null) {
            btnMarkFailed.setVisibility(canFail ? View.VISIBLE : View.GONE);
            btnMarkFailed.setOnClickListener(v -> {
                int checkedId = rg != null ? rg.getCheckedRadioButtonId() : -1;
                String selectedType = "Other problem";
                if (checkedId == R.id.rbVehicle) selectedType = "Vehicle problem";
                else if (checkedId == R.id.rbCustomer) selectedType = "Customer unreachable";
                else if (checkedId == R.id.rbAddress) selectedType = "Incorrect address";
                String details = etDesc != null && etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                String full = selectedType + (details.isEmpty() ? "" : ": " + details);
                String reason = full.length() > 480 ? full.substring(0, 480) : full;
                String bookingText = currentDelivery.getBookingId();
                new AlertDialog.Builder(this)
                        .setTitle("Mark delivery failed?")
                        .setMessage("Booking #" + currentRecord.bookingId + " will be reported as FAILED (" + reason
                                + "). You cannot continue it afterwards; the administrator will review it.")
                        .setPositiveButton("Mark failed", (d2, w2) -> {
                            bottomSheetDialog.dismiss();
                            advanceStatus(DeliveryProgress.FAILED, reason, () ->
                                    IssueReporter.send(this, false, bookingText, "Delivery failed", reason, locationHelper));
                        })
                        .setNegativeButton("Back", null)
                        .show();
            });
        }
        bottomSheetDialog.show();
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
