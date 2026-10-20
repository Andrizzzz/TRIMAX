package com.example.trimax;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrackingMapActivity extends AppCompatActivity {

    private MapView mapView;
    private DatabaseHelper dbHelper;
    private TextView subtitleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        Configuration.getInstance().load(this,
                android.preference.PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue("TRIMAX-DeliveryApp/1.0 (Android; com.example.trimax)");
        File base = new File(getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(base);
        Configuration.getInstance().setOsmdroidTileCache(new File(base, "tiles"));

        dbHelper = new DatabaseHelper(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setFitsSystemWindows(true);

        TextView title = new TextView(this);
        title.setText("Client Tracking Map");
        title.setTextColor(Color.rgb(20, 35, 60));
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(dp(18), dp(18), dp(18), dp(4));
        root.addView(title);

        subtitleTextView = new TextView(this);
        subtitleTextView.setText("All customer pin locations are shown below. Tap any pin to view client details.");
        subtitleTextView.setTextColor(Color.DKGRAY);
        subtitleTextView.setTextSize(13);
        subtitleTextView.setPadding(dp(18), 0, dp(18), dp(12));
        root.addView(subtitleTextView);

        mapView = new MapView(this);
        mapView.setTileSource(MockMapView.OSM_GERMANY);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);
        root.addView(mapView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView back = new TextView(this);
        back.setText("‹  Back to Dashboard");
        back.setTextColor(Color.rgb(37, 99, 235));
        back.setTextSize(15);
        back.setTypeface(null, android.graphics.Typeface.BOLD);
        back.setPadding(dp(18), dp(14), dp(18), dp(18));
        back.setOnClickListener(v -> finish());
        root.addView(back);

        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(), bars.bottom);
            return insets;
        });

        showCustomerPins(subtitleTextView);
    }

    private void showCustomerPins(TextView subtitle) {
        if (mapView == null) return;
        mapView.getOverlays().clear();

        // When opened from Booking Details, show only that booking's saved destination.
        String selectedBookingId = getIntent().getStringExtra("BOOKING_ID");
        if (TextUtils.isEmpty(selectedBookingId)) {
            selectedBookingId = getIntent().getStringExtra("DELIVERY_ID");
        }
        if (!TextUtils.isEmpty(selectedBookingId)) {
            showBookingDestination(selectedBookingId.trim(), subtitle);
            return;
        }

        List<DatabaseHelper.SavedCustomer> customers = dbHelper != null ? dbHelper.getSavedCustomers("") : new ArrayList<>();
        int pinCount = 0;
        int missingLocationCount = 0;
        List<GeoPoint> validPoints = new ArrayList<>();

        if (customers != null) {
            for (DatabaseHelper.SavedCustomer customer : customers) {
                if (customer == null) continue;
                double lat = customer.getLatitude();
                double lng = customer.getLongitude();

                // Never invent a map pin for a customer whose real coordinates are missing.
                if (!isValidCoordinate(lat, lng)) {
                    missingLocationCount++;
                    continue;
                }

                GeoPoint point = new GeoPoint(lat, lng);
                validPoints.add(point);
                Marker marker = new Marker(mapView);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                String name = customer.getName() != null && !customer.getName().trim().isEmpty()
                        ? customer.getName().trim() : "Client";
                marker.setTitle(name);

                StringBuilder snippet = new StringBuilder();
                if (customer.getPhone() != null && !customer.getPhone().trim().isEmpty()) {
                    snippet.append("📞 ").append(customer.getPhone().trim());
                }
                if (customer.getAddress() != null && !customer.getAddress().trim().isEmpty()) {
                    if (snippet.length() > 0) snippet.append("\n");
                    snippet.append("📍 ").append(customer.getAddress().trim());
                }
                if (customer.getBookingCount() > 0) {
                    if (snippet.length() > 0) snippet.append("\n");
                    snippet.append("📦 ").append(customer.getBookingCount()).append(" booking(s)");
                }
                marker.setSnippet(snippet.toString());

                marker.setRelatedObject(customer);
                marker.setOnMarkerClickListener((clicked, map) -> {
                    DatabaseHelper.SavedCustomer c = (DatabaseHelper.SavedCustomer) clicked.getRelatedObject();
                    mapView.getController().animateTo(clicked.getPosition());
                    clicked.showInfoWindow();
                    if (c != null) {
                        showCustomerDetailDialog(c);
                    }
                    return true;
                });
                mapView.getOverlays().add(marker);
                pinCount++;
            }
        }

        if (subtitle != null) {
            if (!validPoints.isEmpty()) {
                String summary = pinCount + " client location pin(s) on map. Tap any pin for details.";
                if (missingLocationCount > 0) {
                    summary += " " + missingLocationCount + " customer(s) have no saved coordinates.";
                }
                subtitle.setText(summary);
            } else if (missingLocationCount > 0) {
                subtitle.setText("No saved customer coordinates are available yet. Update each customer's saved map location.");
            } else {
                subtitle.setText("No customers found. Add customers in Clients or New Booking to view map pins.");
            }
        }

        if (!validPoints.isEmpty()) {
            if (validPoints.size() == 1) {
                mapView.getController().setZoom(15.0);
                mapView.getController().setCenter(validPoints.get(0));
            } else {
                final List<GeoPoint> pointsToBound = validPoints;
                mapView.post(() -> {
                    try {
                        mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(pointsToBound), false, dp(56));
                    } catch (Exception ignored) {
                        if (!pointsToBound.isEmpty()) {
                            mapView.getController().setCenter(pointsToBound.get(0));
                        }
                    }
                });
            }
        } else {
            // No fallback coordinates: leave the map uncentered rather than implying a false location.
            mapView.getController().setZoom(4.0);
        }
        mapView.invalidate();
    }

    private void showBookingDestination(String bookingId, TextView subtitle) {
        Delivery delivery = dbHelper != null ? dbHelper.getDeliveryByCode(bookingId) : null;
        if (delivery == null) {
            if (subtitle != null) subtitle.setText("Booking #" + bookingId + " was not found.");
            mapView.getController().setZoom(4.0);
            mapView.invalidate();
            return;
        }

        double lat = delivery.getDestinationLatitude();
        double lng = delivery.getDestinationLongitude();
        String address = delivery.getDestination();
        if (!isValidCoordinate(lat, lng)) {
            if (subtitle != null) {
                subtitle.setText("No saved map coordinates for booking #" + bookingId
                        + (TextUtils.isEmpty(address) ? "." : ". Destination: " + address));
            }
            mapView.getController().setZoom(4.0);
            mapView.invalidate();
            return;
        }

        GeoPoint point = new GeoPoint(lat, lng);
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(TextUtils.isEmpty(delivery.getCustomerName()) ? "Customer Destination" : delivery.getCustomerName());
        marker.setSnippet((TextUtils.isEmpty(address) ? "Saved booking destination" : address)
                + "\\nBooking #" + bookingId);
        mapView.getOverlays().add(marker);
        mapView.getController().setZoom(16.0);
        mapView.getController().setCenter(point);
        if (subtitle != null) {
            subtitle.setText("Destination for booking #" + bookingId
                    + (TextUtils.isEmpty(address) ? "" : ": " + address));
        }
        mapView.invalidate();
    }

    private void showCustomerDetailDialog(DatabaseHelper.SavedCustomer customer) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(20), dp(20), dp(24));
        layout.setBackgroundColor(Color.WHITE);

        // Header Row with Initials Avatar & Name
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView avatar = new TextView(this);
        String nameStr = customer.getName() != null && !customer.getName().trim().isEmpty() ? customer.getName().trim() : "Client";
        avatar.setText(nameStr.substring(0, 1).toUpperCase());
        avatar.setTextColor(Color.rgb(37, 99, 235));
        avatar.setTextSize(20);
        avatar.setTypeface(null, android.graphics.Typeface.BOLD);
        avatar.setGravity(android.view.Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_icon_badge);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        avatarParams.rightMargin = dp(14);
        headerRow.addView(avatar, avatarParams);

        LinearLayout nameContainer = new LinearLayout(this);
        nameContainer.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(nameStr);
        tvTitle.setTextColor(Color.rgb(20, 35, 60));
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        nameContainer.addView(tvTitle);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("Customer Profile");
        tvSubtitle.setTextColor(Color.GRAY);
        tvSubtitle.setTextSize(12);
        nameContainer.addView(tvSubtitle);

        headerRow.addView(nameContainer);
        layout.addView(headerRow);

        // Contact Phone Row
        if (customer.getPhone() != null && !customer.getPhone().trim().isEmpty()) {
            LinearLayout phoneRow = new LinearLayout(this);
            phoneRow.setOrientation(LinearLayout.HORIZONTAL);
            phoneRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            phoneRow.setPadding(0, dp(14), 0, 0);

            TextView tvPhone = new TextView(this);
            tvPhone.setText("📞 Phone: " + customer.getPhone());
            tvPhone.setTextColor(Color.DKGRAY);
            tvPhone.setTextSize(14);
            phoneRow.addView(tvPhone, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            MaterialButton btnCall = new MaterialButton(this);
            btnCall.setText("Call");
            btnCall.setAllCaps(false);
            btnCall.setTextSize(12);
            btnCall.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(37, 99, 235)));
            btnCall.setOnClickListener(v -> {
                String clean = customer.getPhone().replaceAll("[^0-9]", "");
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + clean)));
            });
            phoneRow.addView(btnCall);
            layout.addView(phoneRow);
        }

        // Email Row
        if (customer.getEmail() != null && !customer.getEmail().trim().isEmpty()) {
            TextView tvEmail = new TextView(this);
            tvEmail.setText("✉ Email: " + customer.getEmail());
            tvEmail.setTextColor(Color.DKGRAY);
            tvEmail.setTextSize(14);
            tvEmail.setPadding(0, dp(6), 0, 0);
            layout.addView(tvEmail);
        }

        // Address Row
        if (customer.getAddress() != null && !customer.getAddress().trim().isEmpty()) {
            TextView tvAddress = new TextView(this);
            tvAddress.setText("📍 Address: " + customer.getAddress());
            tvAddress.setTextColor(Color.DKGRAY);
            tvAddress.setTextSize(14);
            tvAddress.setPadding(0, dp(6), 0, 0);
            layout.addView(tvAddress);
        }

        // Bookings Count & History Preview
        List<Delivery> bookings = dbHelper != null ? dbHelper.getBookingsForCustomer(customer.getPhone(), customer.getName()) : new ArrayList<>();
        TextView tvBookingsHeader = new TextView(this);
        tvBookingsHeader.setText("📦 Bookings: " + (bookings != null ? bookings.size() : customer.getBookingCount()) + " order(s)");
        tvBookingsHeader.setTextColor(Color.rgb(37, 99, 235));
        tvBookingsHeader.setTextSize(14);
        tvBookingsHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvBookingsHeader.setPadding(0, dp(12), 0, dp(8));
        layout.addView(tvBookingsHeader);

        if (bookings != null && !bookings.isEmpty()) {
            Delivery topDelivery = bookings.get(0);
            CardView bookingCard = new CardView(this);
            bookingCard.setCardBackgroundColor(Color.rgb(245, 247, 250));
            bookingCard.setRadius(dp(12));
            bookingCard.setCardElevation(0);
            LinearLayout bookingLayout = new LinearLayout(this);
            bookingLayout.setOrientation(LinearLayout.VERTICAL);
            bookingLayout.setPadding(dp(12), dp(10), dp(12), dp(10));

            TextView tvBkId = new TextView(this);
            tvBkId.setText(String.format(Locale.US, "Latest: #%s • %s", topDelivery.getBookingId(), topDelivery.getStatus()));
            tvBkId.setTextColor(Color.rgb(20, 35, 60));
            tvBkId.setTextSize(13);
            tvBkId.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvBkDest = new TextView(this);
            tvBkDest.setText(String.format(Locale.US, "To: %s (₱ %.2f)", topDelivery.getDestination(), topDelivery.getCurrentTotal()));
            tvBkDest.setTextColor(Color.GRAY);
            tvBkDest.setTextSize(12);

            bookingLayout.addView(tvBkId);
            bookingLayout.addView(tvBkDest);
            bookingCard.addView(bookingLayout);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = dp(14);
            layout.addView(bookingCard, cardParams);
        }

        // View Full Details Action Button
        MaterialButton btnViewFull = new MaterialButton(this);
        btnViewFull.setText("View All Details & Booking History");
        btnViewFull.setAllCaps(false);
        btnViewFull.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(37, 99, 235)));
        btnViewFull.setTextColor(Color.WHITE);
        btnViewFull.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(TrackingMapActivity.this, ClientDetailActivity.class);
            intent.putExtra("CLIENT_NAME", customer.getName());
            intent.putExtra("CLIENT_PHONE", customer.getPhone());
            intent.putExtra("CLIENT_EMAIL", customer.getEmail());
            intent.putExtra("CLIENT_ADDRESS", customer.getAddress());
            intent.putExtra("BOOKINGS_COUNT", String.valueOf(customer.getBookingCount()));
            startActivity(intent);
        });
        layout.addView(btnViewFull, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        bottomSheetDialog.setContentView(layout);
        bottomSheetDialog.show();
    }

    private boolean isValidCoordinate(double lat, double lng) {
        return Double.isFinite(lat) && Double.isFinite(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180
                && !(lat == 0 && lng == 0);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (subtitleTextView != null) showCustomerPins(subtitleTextView);
    }

    @Override protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (mapView != null) mapView.onDetach();
        super.onDestroy();
    }
}
