package com.example.trimax;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Floating liquid-glass bottom navigation bar.
 *
 * Drop-in for the parts of BottomNavigationView this app uses:
 * {@link #setSelectedItemId(int)} and {@link #setOnItemSelectedListener(OnItemSelectedListener)}.
 * Items come from a menu resource ({@code app:navMenu} in XML or {@link #setMenu(int)}).
 * The selected tab sits in a glass bubble that slides and stretches like liquid when the
 * selection changes.
 */
public class LiquidGlassNavBar extends LiquidGlassView {

    /** Same shape as BottomNavigationView's listener, so existing lambdas keep working. */
    public interface OnItemSelectedListener {
        boolean onNavigationItemSelected(MenuItem item);
    }

    private final float dp;
    private final LinearLayout row;
    private final List<MenuItem> items = new ArrayList<>();
    private final List<LinearLayout> itemViews = new ArrayList<>();
    private final List<ImageView> icons = new ArrayList<>();
    private final List<TextView> labels = new ArrayList<>();

    private final Paint bubbleFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubbleTint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubbleRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bubble = new RectF();

    private OnItemSelectedListener listener;
    private int selectedIndex = -1;
    private int pinnedItemId = View.NO_ID;
    private ValueAnimator slide;
    private float fromCenter;
    private float toCenter;
    private float slideProgress = 1f;
    private boolean sliding;

    public LiquidGlassNavBar(Context context) {
        this(context, null);
    }

    public LiquidGlassNavBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiquidGlassNavBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dp = getResources().getDisplayMetrics().density;

        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(Math.round(6 * dp), 0, Math.round(6 * dp), 0);
        addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        bubbleRim.setStyle(Paint.Style.STROKE);
        bubbleRim.setStrokeWidth(1f * dp);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LiquidGlassNavBar);
        int menuRes = a.getResourceId(R.styleable.LiquidGlassNavBar_navMenu, 0);
        a.recycle();
        if (menuRes != 0) {
            setMenu(menuRes);
        }
    }

    // ---------------------------------------------------------------- public API

    public void setMenu(int menuRes) {
        PopupMenu holder = new PopupMenu(getContext(), this);
        holder.inflate(menuRes);
        Menu menu = holder.getMenu();

        row.removeAllViews();
        items.clear();
        itemViews.clear();
        icons.clear();
        labels.clear();
        selectedIndex = -1;

        for (int i = 0; i < menu.size(); i++) {
            addItem(menu.getItem(i));
        }
        if (!items.isEmpty()) {
            int start = indexOf(pinnedItemId);
            jumpTo(start >= 0 ? start : 0);
        }
    }

    public void setOnItemSelectedListener(OnItemSelectedListener l) {
        listener = l;
    }

    /**
     * Marks the tab for the current screen. Does not call the listener, so it is safe to
     * call before or after {@link #setOnItemSelectedListener}.
     */
    public void setSelectedItemId(int itemId) {
        pinnedItemId = itemId;
        int i = indexOf(itemId);
        if (i >= 0) jumpTo(i);
    }

    public int getSelectedItemId() {
        return selectedIndex >= 0 ? items.get(selectedIndex).getItemId() : View.NO_ID;
    }

    // ---------------------------------------------------------------- items

    private void addItem(MenuItem item) {
        Context ctx = getContext();
        final int index = items.size();

        LinearLayout cell = new LinearLayout(ctx);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setClickable(true);
        cell.setFocusable(true);
        cell.setContentDescription(item.getTitle());

        ImageView icon = new ImageView(ctx);
        icon.setImageDrawable(item.getIcon());
        icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        int iconSize = Math.round(24 * dp);
        cell.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        TextView label = new TextView(ctx);
        label.setText(item.getTitle());
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        label.setLetterSpacing(-0.01f);
        label.setMaxLines(1);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        label.setIncludeFontPadding(false);
        label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelLp.topMargin = Math.round(3 * dp);
        cell.addView(label, labelLp);

        cell.setOnClickListener(v -> onItemTapped(index));
        cell.setOnTouchListener(this::onCellTouch);

        row.addView(cell, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        items.add(item);
        itemViews.add(cell);
        icons.add(icon);
        labels.add(label);
        applyColors();
    }

    private boolean onCellTouch(View v, MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                v.animate().scaleX(0.86f).scaleY(0.86f).setDuration(110)
                        .setInterpolator(null).start();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                v.animate().scaleX(1f).scaleY(1f).setDuration(260)
                        .setInterpolator(new OvershootInterpolator(3f)).start();
                break;
            default:
                break;
        }
        return false; // let the click go through
    }

    private void onItemTapped(int index) {
        if (index == selectedIndex) return; // same as BottomNavigationView: no re-select callback
        boolean accept = listener == null || listener.onNavigationItemSelected(items.get(index));
        if (accept) {
            slideTo(index);
        }
    }

    private int indexOf(int itemId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItemId() == itemId) return i;
        }
        return -1;
    }

    private void applyColors() {
        int active = ContextCompat.getColor(getContext(), R.color.primary_blue);
        int inactive = ContextCompat.getColor(getContext(), R.color.text_secondary);
        for (int i = 0; i < items.size(); i++) {
            boolean on = i == selectedIndex;
            int c = on ? active : inactive;
            icons.get(i).setImageTintList(ColorStateList.valueOf(c));
            labels.get(i).setTextColor(c);
            labels.get(i).setTypeface(Typeface.create(on ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
            itemViews.get(i).setSelected(on);
        }
        updateBubblePaints(active);
    }

    // ---------------------------------------------------------------- selection + animation

    private void jumpTo(int index) {
        if (slide != null) slide.cancel();
        sliding = false;
        selectedIndex = index;
        slideProgress = 1f;
        applyColors();
        invalidate();
    }

    private void slideTo(int index) {
        float from = currentBubbleCenter();
        selectedIndex = index;
        applyColors();
        float to = cellCenter(index);
        if (from < 0 || to < 0) {
            jumpTo(index);
            return;
        }
        fromCenter = from;
        toCenter = to;
        if (slide != null) slide.cancel();
        slide = ValueAnimator.ofFloat(0f, 1f);
        slide.setDuration(420);
        slide.setInterpolator(new PathInterpolator(0.3f, 1.35f, 0.4f, 1f)); // slight overshoot
        slide.addUpdateListener(anim -> {
            slideProgress = (float) anim.getAnimatedValue();
            invalidate();
        });
        slide.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                sliding = false;
                slideProgress = 1f;
                invalidate();
            }
        });
        slideProgress = 0f;
        sliding = true;
        slide.start();
    }

    private float cellCenter(int index) {
        if (index < 0 || index >= itemViews.size()) return -1f;
        View cell = itemViews.get(index);
        if (cell.getWidth() == 0) return -1f;
        return row.getLeft() + cell.getLeft() + cell.getWidth() / 2f;
    }

    private float currentBubbleCenter() {
        if (!sliding) return cellCenter(selectedIndex);
        return fromCenter + (toCenter - fromCenter) * slideProgress;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        // Coming back to this screen (e.g. with Back): show this screen's own tab again.
        if (hasWindowFocus && pinnedItemId != View.NO_ID) {
            int i = indexOf(pinnedItemId);
            if (i >= 0 && i != selectedIndex) jumpTo(i);
        }
    }

    // ---------------------------------------------------------------- drawing

    private void updateBubblePaints(int accent) {
        boolean night = isNightMode();
        bubbleFill.setColor(night ? Color.argb(34, 255, 255, 255) : Color.argb(150, 255, 255, 255));
        bubbleTint.setColor(Color.argb(night ? 46 : 24, Color.red(accent), Color.green(accent), Color.blue(accent)));
        bubbleRim.setColor(night ? Color.argb(60, 255, 255, 255) : Color.argb(235, 255, 255, 255));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); // glass layers
        if (selectedIndex < 0 || itemViews.isEmpty()) return;

        float center = currentBubbleCenter();
        if (center < 0) return;
        View cell = itemViews.get(selectedIndex);

        // Liquid stretch: wider and a bit flatter mid-flight, back to round at rest.
        float stretch = !sliding ? 0f
                : (float) Math.sin(Math.PI * Math.max(0f, Math.min(1f, slideProgress)));
        float w = (cell.getWidth() - 4 * dp) * (1f + 0.32f * stretch);
        float h = (getHeight() - 12 * dp) * (1f - 0.08f * stretch);
        float top = (getHeight() - h) / 2f;
        bubble.set(center - w / 2f, top, center + w / 2f, top + h);
        float r = h / 2f;

        canvas.drawRoundRect(bubble, r, r, bubbleFill);
        canvas.drawRoundRect(bubble, r, r, bubbleTint);
        float inset = bubbleRim.getStrokeWidth() / 2f;
        bubble.inset(inset, inset);
        canvas.drawRoundRect(bubble, r - inset, r - inset, bubbleRim);
    }
}
