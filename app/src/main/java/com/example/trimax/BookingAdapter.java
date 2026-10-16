package com.example.trimax;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    public interface OnBookingClickListener {
        void onBookingClick(Delivery delivery);
    }

    private List<Delivery> bookingList = new ArrayList<>();
    private OnBookingClickListener clickListener;

    public BookingAdapter(OnBookingClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setBookingList(List<Delivery> list) {
        this.bookingList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_card, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Delivery booking = bookingList.get(position);
        holder.bind(booking, clickListener);
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingId;
        TextView tvStatusBadge;
        TextView tvCustomerName;
        TextView tvDateTime;
        TextView tvTotalAmount;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
        }

        public void bind(Delivery delivery, OnBookingClickListener listener) {
            if (delivery == null) return;

            tvBookingId.setText(delivery.getBookingId() != null ? delivery.getBookingId() : "N/A");
            tvCustomerName.setText(String.format("Customer: %s", delivery.getCustomerName() != null ? delivery.getCustomerName() : "Unknown"));
            tvDateTime.setText(delivery.getScheduledTime() != null ? delivery.getScheduledTime() : "N/A");
            tvTotalAmount.setText(String.format(Locale.US, "₱ %,.2f", delivery.getCurrentTotal()));

            String status = delivery.getStatus() != null ? delivery.getStatus().toUpperCase(Locale.US) : "PENDING";

            if ("CONFIRMED".equals(status)) {
                tvStatusBadge.setText("CONFIRMED");
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
                tvStatusBadge.setTextColor(0xFF059669);
            } else if ("CANCELLED".equals(status)) {
                tvStatusBadge.setText("CANCELLED");
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_cancelled);
                tvStatusBadge.setTextColor(0xFFEF4444);
            } else if ("COMPLETED".equals(status) || "DELIVERED".equals(status)) {
                tvStatusBadge.setText("COMPLETED");
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_completed);
                tvStatusBadge.setTextColor(0xFF2563EB);
            } else { // PENDING / ASSIGNED / default
                tvStatusBadge.setText("PENDING");
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
                tvStatusBadge.setTextColor(0xFFD97706);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBookingClick(delivery);
                }
            });
        }
    }
}
