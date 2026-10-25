package com.example.trimax;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.core.content.ContextCompat;

/**
 * The bottom nav's selected-tab bubble, drawn as a drop of liquid glass.
 *
 * The activities move it exactly as before (translationX animations). This view watches its
 * own position every frame and, while it is moving, stretches wider and flatter in proportion
 * to its speed, then settles back into a round capsule when it stops.
 */
public class LiquidPillView extends View {

    private static final float MAX_STRETCH = 0.45f;

    private final float dp;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF pill = new RectF();

    private float lastX = Float.NaN;
    private long lastTime;
    private float stretch;
    private float shaderHeight = -1f;

    private final ViewTreeObserver.OnPreDrawListener tracker = () -> {
        track();
        return true;
    };

    public LiquidPillView(Context context) {
        this(context, null);
    }

    public LiquidPillView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiquidPillView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        dp = getResources().getDisplayMetrics().density;
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1f * dp);
        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        fill.setColor(ContextCompat.getColor(context, R.color.nav_selected_bg));
        rim.setColor(night ? Color.argb(70, 255, 255, 255) : Color.argb(235, 255, 255, 255));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnPreDrawListener(tracker);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnPreDrawListener(tracker);
        super.onDetachedFromWindow();
    }

    /** Runs once per frame. Animations move this view on the render thread, so poll here. */
    private void track() {
        if (!isLaidOut()) return;
        float x = getTranslationX();
        long now = SystemClock.uptimeMillis();
        if (Float.isNaN(lastX)) {
            lastX = x;
            lastTime = now;
            return;
        }
        long dt = Math.max(8, now - lastTime);
        float speed = Math.abs(x - lastX) / dt;              // px per ms
        lastX = x;
        lastTime = now;

        float target = dt > 120 ? 0f : Math.min(MAX_STRETCH, speed / (2.5f * dp));
        float next = stretch + (target - stretch) * (target > stretch ? 0.6f : 0.25f);
        if (next < 0.004f) next = 0f;
        if (next != stretch) {
            stretch = next;
            invalidate();
            postInvalidateOnAnimation(); // keep frames coming until it settles
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth() * (1f + stretch);
        float h = getHeight() * (1f - 0.22f * stretch);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        pill.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
        float r = h / 2f;

        if (shaderHeight != getHeight()) {
            shaderHeight = getHeight();
            boolean night = (getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            sheen.setShader(new LinearGradient(0, 0, 0, getHeight() * 0.65f,
                    Color.argb(night ? 40 : 140, 255, 255, 255), Color.TRANSPARENT,
                    Shader.TileMode.CLAMP));
        }

        canvas.drawRoundRect(pill, r, r, fill);
        canvas.drawRoundRect(pill, r, r, sheen);
        float in = rim.getStrokeWidth() / 2f;
        pill.inset(in, in);
        canvas.drawRoundRect(pill, r - in, r - in, rim);
    }
}
