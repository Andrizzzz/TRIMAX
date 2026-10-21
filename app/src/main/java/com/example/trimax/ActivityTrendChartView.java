package com.example.trimax;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight, theme-aware booking trend chart. Values are supplied from actual
 * booking records by ReportsActivity; no placeholder or sample values are drawn.
 */
public class ActivityTrendChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<String> labels = new ArrayList<>();
    private List<Integer> values = new ArrayList<>();

    public ActivityTrendChartView(Context context) {
        super(context);
        init();
    }

    public ActivityTrendChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ActivityTrendChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setChartData(List<String> newLabels, List<Integer> newValues) {
        labels = newLabels == null ? new ArrayList<>() : new ArrayList<>(newLabels);
        values = newValues == null ? new ArrayList<>() : new ArrayList<>(newValues);
        invalidate();
    }

    private int color(int resourceId) {
        return ContextCompat.getColor(getContext(), resourceId);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float left = 38f * density;
        float right = getWidth() - 12f * density;
        float top = 14f * density;
        float bottom = getHeight() - 30f * density;
        float chartWidth = Math.max(1f, right - left);
        float chartHeight = Math.max(1f, bottom - top);

        int textColor = color(R.color.text_secondary);
        int gridColor = color(R.color.border_subtle);
        int lineColor = color(R.color.primary_blue);
        int surfaceColor = color(R.color.surface_alt);

        if (labels.isEmpty() || values.isEmpty()) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(textColor);
            paint.setTextSize(13f * density);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No booking activity for this period", getWidth() / 2f,
                    getHeight() / 2f, paint);
            return;
        }

        int maxValue = 0;
        for (Integer value : values) if (value != null) maxValue = Math.max(maxValue, value);
        float scaleMax = Math.max(1, maxValue);

        paint.setStrokeWidth(1f * density);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(gridColor);
        for (int i = 0; i <= 3; i++) {
            float y = top + chartHeight * i / 3f;
            canvas.drawLine(left, y, right, y, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(textColor);
            paint.setTextSize(10f * density);
            paint.setTextAlign(Paint.Align.RIGHT);
            int tick = Math.round(scaleMax * (3 - i) / 3f);
            canvas.drawText(String.valueOf(tick), left - 8f * density, y + 3.5f * density, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(gridColor);
        }

        float stepX = labels.size() == 1 ? 0 : chartWidth / (labels.size() - 1f);
        Path line = new Path();
        Path fill = new Path();
        for (int i = 0; i < values.size(); i++) {
            float x = left + i * stepX;
            float y = bottom - (values.get(i) == null ? 0 : values.get(i)) / scaleMax * chartHeight;
            if (i == 0) {
                line.moveTo(x, y);
                fill.moveTo(x, bottom);
                fill.lineTo(x, y);
            } else {
                line.lineTo(x, y);
                fill.lineTo(x, y);
            }
        }
        fill.lineTo(left + (values.size() - 1) * stepX, bottom);
        fill.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(surfaceColor);
        canvas.drawPath(fill, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f * density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(lineColor);
        canvas.drawPath(line, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(lineColor);
        for (int i = 0; i < values.size(); i++) {
            float x = left + i * stepX;
            float y = bottom - (values.get(i) == null ? 0 : values.get(i)) / scaleMax * chartHeight;
            canvas.drawCircle(x, y, 3.2f * density, paint);
        }

        paint.setColor(textColor);
        paint.setTextSize(9f * density);
        paint.setTextAlign(Paint.Align.CENTER);
        int labelStep = Math.max(1, (int) Math.ceil(labels.size() / 7.0));
        for (int i = 0; i < labels.size(); i += labelStep) {
            float x = left + i * stepX;
            canvas.drawText(labels.get(i), x, getHeight() - 8f * density, paint);
        }
        if (labels.size() > 1 && (labels.size() - 1) % labelStep != 0) {
            canvas.drawText(labels.get(labels.size() - 1), right, getHeight() - 8f * density, paint);
        }
    }
}
