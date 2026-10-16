package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class DriverNotificationsActivity extends AppCompatActivity {

    private SessionManager session;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_notifications);

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
        CardView cardDriverNotif1 = findViewById(R.id.cardDriverNotif1);
        CardView cardDriverNotif2 = findViewById(R.id.cardDriverNotif2);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        List<Delivery> assigned = dbHelper.getAssignedDeliveries(session.getUsername());
        String deliveryId1 = (assigned != null && assigned.size() > 0) ? assigned.get(0).getBookingId() : "TRX-1024";
        String deliveryId2 = (assigned != null && assigned.size() > 1) ? assigned.get(1).getBookingId() : "TRX-1025";

        if (cardDriverNotif1 != null) {
            cardDriverNotif1.setOnClickListener(v -> {
                Intent intent = new Intent(this, DriverDeliveryDetailsActivity.class);
                intent.putExtra("DELIVERY_ID", deliveryId1);
                startActivity(intent);
            });
        }

        if (cardDriverNotif2 != null) {
            cardDriverNotif2.setOnClickListener(v -> {
                Intent intent = new Intent(this, DriverDeliveryDetailsActivity.class);
                intent.putExtra("DELIVERY_ID", deliveryId2);
                startActivity(intent);
            });
        }
    }
}
