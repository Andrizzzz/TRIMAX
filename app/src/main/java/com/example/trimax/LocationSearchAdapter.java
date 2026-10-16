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
        super(context, android.R.layout.simple_dropdown_item_1line, items);
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
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        }

        SearchResultItem item = getItem(position);
        if (item != null) {
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            if (text1 != null) {
                text1.setText(item.getShortName());
                text1.setTextColor(0xFF0F172A);
                text1.setTextSize(14);
                text1.setTypeface(null, android.graphics.Typeface.BOLD);
            }

            if (text2 != null) {
                text2.setText(item.getFullAddress());
                text2.setTextColor(0xFF64748B);
                text2.setTextSize(12);
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
