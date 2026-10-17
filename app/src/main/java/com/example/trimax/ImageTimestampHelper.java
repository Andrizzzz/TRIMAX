package com.example.trimax;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageTimestampHelper {

    private static final String TAG = "ImageTimestampHelper";

    /**
     * Overlays a clean date, time, and GPS coordinate banner onto a Bitmap and saves it to storage.
     *
     * @param context Application context
     * @param srcBitmap Source bitmap from camera capture
     * @param prefix Label prefix (e.g. "DUTY START" or "DELIVERY PROOF")
     * @param lat Optional latitude
     * @param lng Optional longitude
     * @return Saved File object or null on failure
     */
    public static File saveBitmapWithTimestamp(Context context, Bitmap srcBitmap, String prefix, double lat, double lng) {
        if (srcBitmap == null) return null;

        Bitmap mutableBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        int width = mutableBitmap.getWidth();
        int height = mutableBitmap.getHeight();

        // Calculate responsive text size and banner height based on bitmap dimensions
        int fontSize = Math.max(16, width / 28);
        int bannerHeight = fontSize * 3;

        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#CC0F172A")); // Semi-transparent dark navy
        bgPaint.setStyle(Paint.Style.FILL);

        // Draw banner at bottom of image
        canvas.drawRect(0, height - bannerHeight, width, height, bgPaint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(fontSize);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US);
        String timestampStr = sdf.format(new Date());

        String line1 = (prefix != null ? prefix + " • " : "") + timestampStr;
        String line2 = String.format(Locale.US, "GPS: %.4f, %.4f | TRI-MAX Mobile", lat, lng);

        int paddingLeft = Math.max(12, width / 40);
        int line1Y = height - bannerHeight + (fontSize + fontSize / 2);
        int line2Y = line1Y + fontSize + (fontSize / 3);

        canvas.drawText(line1, paddingLeft, line1Y, textPaint);

        Paint subTextPaint = new Paint(textPaint);
        subTextPaint.setTextSize((float) (fontSize * 0.85));
        subTextPaint.setColor(Color.parseColor("#93C5FD")); // Light Accent Blue
        canvas.drawText(line2, paddingLeft, line2Y, subTextPaint);

        // Save timestamped image file
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            storageDir = context.getCacheDir();
        }

        String filename = "TRIMAX_" + (prefix != null ? prefix.replaceAll("[^A-Za-z0-9]", "_") : "IMG") + "_" + System.currentTimeMillis() + ".jpg";
        File imageFile = new File(storageDir, filename);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(imageFile);
            mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            Log.d(TAG, "Timestamped image saved successfully: " + imageFile.getAbsolutePath());
            return imageFile;
        } catch (IOException e) {
            Log.e(TAG, "Failed to save timestamped image: " + e.getMessage(), e);
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
