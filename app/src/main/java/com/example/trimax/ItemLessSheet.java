package com.example.trimax;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * The "Less per piece" sheet for one item, opened from the small Less button under the item's price on the
 * New Booking item list. A slider (0 to the most the Admin allows), quick amounts and an exact-amount box
 * all move together, with a live old → new price. The result is only a preview: the review step and the
 * server check it again.
 */
final class ItemLessSheet {

    interface Listener {
        /** The amount per piece chosen (zero = no discount). */
        void onLess(BigDecimal perUnit);
    }

    private ItemLessSheet() {
    }

    static void show(AppCompatActivity activity, ItemDiscounts.Line line, BigDecimal current, Listener listener) {
        if (!line.allowed) return;
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.sheet_item_less, null);
        dialog.setContentView(view);
        View parent = (View) view.getParent();
        if (parent != null) parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);

        TextView title = view.findViewById(R.id.tvLessTitle);
        TextView subtitle = view.findViewById(R.id.tvLessSubtitle);
        TextView oldPrice = view.findViewById(R.id.tvLessOldPrice);
        TextView newPrice = view.findViewById(R.id.tvLessNewPrice);
        TextView amount = view.findViewById(R.id.tvLessAmount);
        TextView lineText = view.findViewById(R.id.tvLessLine);
        Slider slider = view.findViewById(R.id.sliderLess);
        ChipGroup quick = view.findViewById(R.id.chipGroupLess);
        TextInputLayout exactLayout = view.findViewById(R.id.tilLessExact);
        TextInputEditText exact = view.findViewById(R.id.etLessExact);
        MaterialButton remove = view.findViewById(R.id.btnLessRemove);
        MaterialButton apply = view.findViewById(R.id.btnLessApply);

        title.setText("Less on " + line.name);
        subtitle.setText(DiscountCalc.peso(line.unitPrice) + " each · up to " + DiscountCalc.peso(line.maxPerUnit)
                + " off per piece");

        final BigDecimal[] chosen = {current == null || ItemDiscounts.problem(line, current) != null
                ? ItemDiscounts.scale(BigDecimal.ZERO) : ItemDiscounts.scale(current)};
        final boolean[] syncing = {false};

        Runnable preview = () -> {
            BigDecimal less = chosen[0];
            BigDecimal net = ItemDiscounts.net(line.unitPrice, less);
            boolean on = less.signum() > 0;
            oldPrice.setText(DiscountCalc.peso(line.unitPrice));
            strike(oldPrice, on);
            newPrice.setText(DiscountCalc.peso(net));
            amount.setText(on ? "− " + DiscountCalc.peso(less) : "No less");
            amount.setAlpha(on ? 1f : 0.55f);
            int qty = Math.max(1, line.quantity);
            BigDecimal q = BigDecimal.valueOf(qty);
            lineText.setText(String.format(Locale.US, "%d pc%s: %s → %s", qty, qty == 1 ? "" : "s",
                    DiscountCalc.peso(line.unitPrice.multiply(q)), DiscountCalc.peso(net.multiply(q))));
            apply.setText(on ? "Apply " + DiscountCalc.peso(less) + " less" : "Apply");
            apply.setEnabled(true);
            for (int i = 0; i < quick.getChildCount(); i++) {
                Chip c = (Chip) quick.getChildAt(i);
                Object tag = c.getTag();
                c.setChecked(tag instanceof BigDecimal && ((BigDecimal) tag).compareTo(less) == 0);
            }
        };

        // Slider: 0 .. maximum, snapped to ₱0.25 (the very end is the maximum itself).
        slider.setValueFrom(0f);
        slider.setValueTo(line.maxPerUnit.floatValue());
        slider.setValue(Math.min(chosen[0].floatValue(), line.maxPerUnit.floatValue()));
        slider.setLabelFormatter(v -> DiscountCalc.peso(ItemDiscounts.snap(v, line.maxPerUnit)));
        slider.addOnChangeListener((s, value, fromUser) -> {
            if (!fromUser || syncing[0]) return;
            chosen[0] = ItemDiscounts.snap(value, line.maxPerUnit);
            setExact(exact, chosen[0], syncing);
            exactLayout.setError(null);
            preview.run();
        });

        // Quick amounts: ₱1, ₱2, ... and Max.
        java.util.List<BigDecimal> amounts = ItemDiscounts.quickAmounts(line.maxPerUnit);
        for (int i = 0; i < amounts.size(); i++) {
            BigDecimal a = amounts.get(i);
            boolean isMax = i == amounts.size() - 1;
            Chip chip = new Chip(activity);
            chip.setText(isMax ? "Max " + DiscountCalc.peso(a) : DiscountCalc.peso(a));
            chip.setTag(a);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipBackgroundColor(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{ContextCompat.getColor(activity, R.color.primary_blue),
                            ContextCompat.getColor(activity, R.color.surface_alt)}));
            chip.setTextColor(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{ContextCompat.getColor(activity, R.color.text_on_primary),
                            ContextCompat.getColor(activity, R.color.text_primary)}));
            chip.setChipStrokeWidth(0f);
            chip.setOnClickListener(v -> {
                chosen[0] = a;
                syncing[0] = true;
                slider.setValue(a.floatValue());
                syncing[0] = false;
                setExact(exact, a, syncing);
                exactLayout.setError(null);
                preview.run();
            });
            quick.addView(chip);
        }

        // Exact amount for anything the slider steps miss (e.g. ₱1.10).
        setExact(exact, chosen[0], syncing);
        exact.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (syncing[0]) return;
                String problem;
                BigDecimal v = null;
                try {
                    v = ItemDiscounts.parse(s.toString());
                    problem = ItemDiscounts.problem(line, v);
                } catch (IllegalArgumentException bad) {
                    problem = bad.getMessage();
                }
                exactLayout.setError(problem);
                apply.setEnabled(problem == null);
                if (problem != null) return;
                chosen[0] = ItemDiscounts.scale(v);
                syncing[0] = true;
                slider.setValue(chosen[0].floatValue());
                syncing[0] = false;
                preview.run();
            }
        });

        remove.setOnClickListener(v -> {
            listener.onLess(ItemDiscounts.scale(BigDecimal.ZERO));
            dialog.dismiss();
        });
        apply.setOnClickListener(v -> {
            listener.onLess(chosen[0]);
            dialog.dismiss();
        });

        preview.run();
        dialog.show();
    }

    private static void setExact(TextInputEditText exact, BigDecimal value, boolean[] syncing) {
        syncing[0] = true;
        exact.setText(value.signum() > 0 ? value.toPlainString() : "");
        if (exact.getText() != null) exact.setSelection(exact.getText().length());
        syncing[0] = false;
    }

    static void strike(TextView tv, boolean on) {
        int flags = tv.getPaintFlags();
        tv.setPaintFlags(on ? flags | Paint.STRIKE_THRU_TEXT_FLAG : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }
}
