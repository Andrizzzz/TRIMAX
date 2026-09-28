package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ClientDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_client_detail);

        Intent intent = getIntent();
        String name = intent.getStringExtra("CLIENT_NAME");
        String phone = intent.getStringExtra("CLIENT_PHONE");
        String email = intent.getStringExtra("CLIENT_EMAIL");

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvClientName = findViewById(R.id.tvClientName);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvEmail = findViewById(R.id.tvEmail);

        btnBack.setOnClickListener(v -> finish());

        if (name != null) tvClientName.setText(name);
        if (phone != null) tvPhone.setText("📞 " + phone);
        if (email != null) tvEmail.setText("✉ " + email);
    }
}