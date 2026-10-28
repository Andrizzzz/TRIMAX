package com.example.trimax;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The per-item discount fields (one "less per piece" box for each item the Admin allows; the other items
 * are listed with why they cannot be discounted). Used by the booking summary and by "Change discount"
 * on a Pending booking. The numbers here are only a preview; the server checks every line again.
 */
final class ItemDiscountRows {

    private final Context context;
    private final LinearLayout container;
    private final Runnable onChange;
    private final List<ItemDiscounts.Line> lines = new ArrayList<>();
    private final Map<Integer, TextInputLayout> layouts = new LinkedHashMap<>();
    private final Map<Integer, TextInputEditText> fields = new LinkedHashMap<>();

    ItemDiscountRows(Context context, LinearLayout container, Runnable onChange) {
        this.context = context;
        this.container = container;
        this.onChange = onChange;
    }

    List<ItemDiscounts.Line> lines() {
        return lines;
    }

    boolean anyAllowed() {
        for (ItemDiscounts.Line l : lines) if (l.allowed) return true;
        return false;
    }

    /** Builds the rows; initial = amounts per piece already on the booking (may be null). */
    void show(List<ItemDiscounts.Line> quoteLines, Map<Integer, BigDecimal> initial) {
        container.removeAllViews();
        lines.clear();
        layouts.clear();
        fields.clear();
        lines.addAll(quoteLines);
        for (ItemDiscounts.Line l : lines) {
            TextView title = new TextView(context);
            title.setText(l.title());
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
            title.setPadding(0, dp(10), 0, dp(2));
            container.addView(title);
            if (!l.allowed) {
                TextView why = new TextView(context);
                why.setText(l.reason != null && !l.reason.isEmpty() ? l.reason : "No discount on this item.");
                why.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                why.setAlpha(0.7f);
                container.addView(why);
                continue;
            }
            TextInputLayout til = new TextInputLayout(context);
            til.setHint(l.hint());
            TextInputEditText et = new TextInputEditText(til.getContext());
            et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
            BigDecimal start = initial == null ? null : initial.get(l.key());
            if (start != null && start.signum() > 0) et.setText(start.stripTrailingZeros().toPlainString());
            et.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    check(l);
                    if (onChange != null) onChange.run();
                }
            });
            til.addView(et);
            container.addView(til, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            layouts.put(l.key(), til);
            fields.put(l.key(), et);
        }
        container.setVisibility(lines.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /** The amounts typed so far ({line key -> pesos per piece}); lines that cannot be read are left out. */
    Map<Integer, BigDecimal> values() {
        Map<Integer, BigDecimal> out = new LinkedHashMap<>();
        for (Map.Entry<Integer, TextInputEditText> e : fields.entrySet()) {
            try {
                BigDecimal v = ItemDiscounts.parse(text(e.getValue()));
                if (v != null) out.put(e.getKey(), v);
            } catch (IllegalArgumentException ignored) {
                // shown on the field by check()
            }
        }
        return out;
    }

    /** Checks every line, shows errors on the fields, returns the first problem (null when all are fine). */
    String firstProblem() {
        String first = null;
        for (ItemDiscounts.Line l : lines) {
            String p = check(l);
            if (p != null && first == null) {
                first = p;
                TextInputEditText et = fields.get(l.key());
                if (et != null) et.requestFocus();
            }
        }
        return first;
    }

    private String check(ItemDiscounts.Line l) {
        TextInputLayout til = layouts.get(l.key());
        TextInputEditText et = fields.get(l.key());
        if (til == null || et == null) return null;
        String problem;
        try {
            problem = ItemDiscounts.problem(l, ItemDiscounts.parse(text(et)));
        } catch (IllegalArgumentException bad) {
            problem = bad.getMessage();
        }
        til.setError(problem);
        return problem;
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    private int dp(int v) {
        return Math.round(v * context.getResources().getDisplayMetrics().density);
    }
}
