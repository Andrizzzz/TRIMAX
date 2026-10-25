package com.example.trimax;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

/**
 * A rounded "liquid glass" surface: it blurs whatever is drawn behind it, then layers a
 * translucent tint, a top sheen and a bright rim on top, like iOS liquid glass.
 *
 * The backdrop is captured by drawing the window's view tree into a small bitmap on every
 * frame (only the area under this view), then box-blurring it. Works from API 24 up and
 * falls back to a frosted tint if a view in the tree cannot be drawn in software.
 *
 * Children may draw outside their own bounds (the nav's liquid pill stretches past its
 * width while it moves), so child clipping is off; the rounded outline still clips everything.
 */
public class LiquidGlassView extends FrameLayout {

    private static final float DOWNSAMPLE = 1f / 6f;
    private static final int BLUR_RADIUS = 5;      // in downsampled pixels
    private static final int BLUR_PASSES = 2;

    private final float density;
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint tintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hairlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final RectF rimBounds = new RectF();
    private final Path clipPath = new Path();
    private final Rect srcRect = new Rect();
    private final int[] myLocation = new int[2];
    private final int[] rootLocation = new int[2];

    private Bitmap backdrop;
    private Canvas backdropCanvas;
    private int[] pixels;
    private int[] scratch;
    private boolean capturing;
    private boolean blurEnabled = true;
    private boolean hasBackdrop;
    private float cornerRadius = -1f; // < 0 means "pill": half the height

    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        captureBackdrop();
        return true;
    };

    public LiquidGlassView(Context context) {
        this(context, null);
    }

    public LiquidGlassView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LiquidGlassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        setWillNotDraw(false);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius());
            }
        });
        setClipChildren(false);

        ColorMatrix saturate = new ColorMatrix();
        saturate.setSaturation(1.35f);
        bitmapPaint.setColorFilter(new ColorMatrixColorFilter(saturate));

        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(1.2f * density);
        hairlinePaint.setStyle(Paint.Style.STROKE);
        hairlinePaint.setStrokeWidth(0.8f * density);
    }

    /** Sets a fixed corner radius in pixels. Pass a negative value for a full pill shape. */
    public void setCornerRadius(float radiusPx) {
        cornerRadius = radiusPx;
        invalidateOutline();
        updateShaders();
        invalidate();
    }

    protected float radius() {
        float pill = getHeight() / 2f;
        return cornerRadius < 0 ? pill : Math.min(cornerRadius, pill);
    }

    protected boolean isNightMode() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            getViewTreeObserver().addOnPreDrawListener(preDrawListener);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
        if (backdrop != null) {
            backdrop.recycle();
            backdrop = null;
            backdropCanvas = null;
        }
        hasBackdrop = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateShaders();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateShaders();
        invalidate();
    }


    // ---------------------------------------------------------------- backdrop capture

    private void captureBackdrop() {
        if (!blurEnabled || !isShown() || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        View root = getRootView();
        if (root == null || root == this) {
            return;
        }

        int bw = Math.max(1, Math.round(getWidth() * DOWNSAMPLE));
        int bh = Math.max(1, Math.round(getHeight() * DOWNSAMPLE));
        if (backdrop == null || backdrop.getWidth() != bw || backdrop.getHeight() != bh) {
            if (backdrop != null) backdrop.recycle();
            backdrop = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            backdropCanvas = new Canvas(backdrop);
            pixels = new int[bw * bh];
            scratch = new int[bw * bh];
        }

        getLocationInWindow(myLocation);
        root.getLocationInWindow(rootLocation);
        float dx = myLocation[0] - rootLocation[0];
        float dy = myLocation[1] - rootLocation[1];

        backdrop.eraseColor(isNightMode() ? Color.rgb(15, 23, 42) : Color.rgb(248, 250, 252));
        int save = backdropCanvas.save();
        backdropCanvas.scale(bw / (float) getWidth(), bh / (float) getHeight());
        backdropCanvas.translate(-dx, -dy);
        capturing = true;
        try {
            root.draw(backdropCanvas);
            hasBackdrop = true;
        } catch (Throwable t) {
            // e.g. a hardware-only bitmap somewhere in the tree: keep the frosted fallback
            blurEnabled = false;
            hasBackdrop = false;
        } finally {
            capturing = false;
            backdropCanvas.restoreToCount(save);
        }

        if (hasBackdrop) {
            backdrop.getPixels(pixels, 0, bw, 0, 0, bw, bh);
            for (int i = 0; i < BLUR_PASSES; i++) {
                boxBlur(pixels, scratch, bw, bh, BLUR_RADIUS);
            }
            backdrop.setPixels(pixels, 0, bw, 0, 0, bw, bh);
            invalidate();
        }
    }

    /** One horizontal + one vertical box-blur pass, in place on {@code px}. */
    private static void boxBlur(int[] px, int[] tmp, int w, int h, int r) {
        blurLine(px, tmp, w, h, r, true);
        blurLine(tmp, px, h, w, r, false);
    }

    /**
     * Blurs rows of {@code in} into {@code out}. When {@code horizontal} is false the image is
     * read as transposed (w/h swapped by the caller) so the same loop does the vertical pass.
     */
    private static void blurLine(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int window = r * 2 + 1;
        int srcW = horizontal ? w : h; // row stride of the source image
        for (int y = 0; y < h; y++) {
            int a = 0, rr = 0, g = 0, b = 0;
            for (int i = -r; i <= r; i++) {
                int c = in[index(clamp(i, w - 1), y, srcW, horizontal)];
                a += c >>> 24; rr += (c >> 16) & 0xFF; g += (c >> 8) & 0xFF; b += c & 0xFF;
            }
            for (int x = 0; x < w; x++) {
                out[index(x, y, srcW, horizontal)] =
                        ((a / window) << 24) | ((rr / window) << 16) | ((g / window) << 8) | (b / window);
                int cOut = in[index(clamp(x - r, w - 1), y, srcW, horizontal)];
                int cIn = in[index(clamp(x + r + 1, w - 1), y, srcW, horizontal)];
                a += (cIn >>> 24) - (cOut >>> 24);
                rr += ((cIn >> 16) & 0xFF) - ((cOut >> 16) & 0xFF);
                g += ((cIn >> 8) & 0xFF) - ((cOut >> 8) & 0xFF);
                b += (cIn & 0xFF) - (cOut & 0xFF);
            }
        }
    }

    private static int index(int x, int y, int stride, boolean horizontal) {
        return horizontal ? y * stride + x : x * stride + y;
    }

    private static int clamp(int v, int max) {
        return v < 0 ? 0 : (v > max ? max : v);
    }

    // ---------------------------------------------------------------- drawing

    private void updateShaders() {
        int h = getHeight();
        if (h == 0) return;
        boolean night = isNightMode();

        sheenPaint.setShader(new LinearGradient(0, 0, 0, h * 0.6f,
                Color.argb(night ? 30 : 95, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP));

        rimPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{
                        Color.argb(night ? 80 : 240, 255, 255, 255),
                        Color.argb(night ? 18 : 70, 255, 255, 255),
                        Color.argb(night ? 40 : 150, 255, 255, 255)},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));

        hairlinePaint.setColor(night ? Color.argb(90, 0, 0, 0) : Color.argb(22, 15, 23, 42));
    }

    @Override
    public void draw(Canvas canvas) {
        // While the backdrop is being captured this view must not draw itself into it.
        if (capturing) return;
        super.draw(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (capturing) return;
        super.dispatchDraw(canvas);
        drawRim(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = radius();
        bounds.set(0, 0, getWidth(), getHeight());
        clipPath.reset();
        clipPath.addRoundRect(bounds, r, r, Path.Direction.CW);

        int save = canvas.save();
        canvas.clipPath(clipPath);

        // tint alpha depends on whether a real backdrop exists (ultra-low opacity for glass transparency)
        boolean night = isNightMode();
        tintPaint.setColor(night ? Color.argb(hasBackdrop ? 35 : 90, 15, 23, 42)
                : Color.argb(hasBackdrop ? 25 : 80, 255, 255, 255));

        if (hasBackdrop && backdrop != null && !backdrop.isRecycled()) {
            srcRect.set(0, 0, backdrop.getWidth(), backdrop.getHeight());
            canvas.drawBitmap(backdrop, srcRect, bounds, bitmapPaint);
        }
        canvas.drawRect(bounds, tintPaint);
        canvas.drawRect(bounds, sheenPaint);
        canvas.restoreToCount(save);
        super.onDraw(canvas);
    }

    private void drawRim(Canvas canvas) {
        float r = radius();
        float inset = rimPaint.getStrokeWidth() / 2f;
        rimBounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
        canvas.drawRoundRect(rimBounds, r - inset, r - inset, hairlinePaint);
        canvas.drawRoundRect(rimBounds, r - inset, r - inset, rimPaint);
    }
}
