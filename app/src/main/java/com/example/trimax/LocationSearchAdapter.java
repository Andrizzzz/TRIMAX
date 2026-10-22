package com.example.trimax;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LocationSearchAdapter extends ArrayAdapter<SearchResultItem> {

    private final Context context;
    private List<SearchResultItem> items;

    public LocationSearchAdapter(@NonNull Context context, @NonNull List<SearchResultItem> items) {
        super(context, R.layout.item_search_suggestion, items);
        this.context = context;
        this.items = new ArrayList<>(items);
    }

    public void updateItems(List<SearchResultItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Nullable
    @Override
    public SearchResultItem getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_search_suggestion, parent, false);
        }

        SearchResultItem item = getItem(position);
        if (item != null) {
            TextView tvShort = convertView.findViewById(R.id.tvSearchShortName);
            TextView tvFull = convertView.findViewById(R.id.tvSearchFullAddress);

            if (tvShort != null) {
                tvShort.setText(item.getShortName());
            }

            if (tvFull != null) {
                tvFull.setText(item.getFullAddress());
            }
        }

        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                results.values = items;
                results.count = items.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }
}
