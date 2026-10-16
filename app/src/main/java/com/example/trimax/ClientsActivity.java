package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class ClientsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clients);

        DatabaseHelper dbHelper = new DatabaseHelper(this);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), 0);
                return insets;
            });
        }

        CardView cardClient1 = findViewById(R.id.cardClient1);
        CardView cardClient2 = findViewById(R.id.cardClient2);
        TextInputEditText etSearch = findViewById(R.id.etSearch);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        if (bottomNavigation != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
            bottomNavigation.setSelectedItemId(R.id.nav_clients);
            setupBottomNavigation(bottomNavigation);
        }

        List<DatabaseHelper.ClientItem> clients = dbHelper.getUniqueClients();
        if (clients != null && !clients.isEmpty()) {
            if (cardClient1 != null) {
                DatabaseHelper.ClientItem c1 = clients.get(0);
                TextView tv1 = cardClient1.findViewById(R.id.tvName1);
                if (tv1 != null) tv1.setText(c1.getName());
                cardClient1.setOnClickListener(v -> openClientDetail(c1.getName(), c1.getPhone(), c1.getEmail(), String.valueOf(c1.getBookingCount())));
            }
            if (clients.size() > 1 && cardClient2 != null) {
                DatabaseHelper.ClientItem c2 = clients.get(1);
                TextView tv2 = cardClient2.findViewById(R.id.tvName2);
                if (tv2 != null) tv2.setText(c2.getName());
                cardClient2.setOnClickListener(v -> openClientDetail(c2.getName(), c2.getPhone(), c2.getEmail(), String.valueOf(c2.getBookingCount())));
            }
        } else {
            if (cardClient1 != null) cardClient1.setOnClickListener(v -> openClientDetail("Juan Dela Cruz", "09171234567", "juan@gmail.com", "3"));
            if (cardClient2 != null) cardClient2.setOnClickListener(v -> openClientDetail("Maria Santos", "09189876543", "maria@gmail.com", "5"));
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s != null ? s.toString().trim().toLowerCase() : "";
                    if (query.isEmpty()) {
                        if (cardClient1 != null) cardClient1.setVisibility(View.VISIBLE);
                        if (cardClient2 != null) cardClient2.setVisibility(View.VISIBLE);
                    } else {
                        boolean match1 = "juan dela cruz".contains(query) || "09171234567".contains(query);
                        boolean match2 = "maria santos".contains(query) || "09189876543".contains(query);

                        if (cardClient1 != null) cardClient1.setVisibility(match1 ? View.VISIBLE : View.GONE);
                        if (cardClient2 != null) cardClient2.setVisibility(match2 ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void openClientDetail(String name, String phone, String email, String count) {
        Intent intent = new Intent(ClientsActivity.this, ClientDetailActivity.class);
        intent.putExtra("CLIENT_NAME", name);
        intent.putExtra("CLIENT_PHONE", phone);
        intent.putExtra("CLIENT_EMAIL", email);
        intent.putExtra("BOOKINGS_COUNT", count);
        startActivity(intent);
    }

    private void setupBottomNavigation(BottomNavigationView bottomNavigation) {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                startActivity(new Intent(this, BookingsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_clients) {
                return true;
            } else if (itemId == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
