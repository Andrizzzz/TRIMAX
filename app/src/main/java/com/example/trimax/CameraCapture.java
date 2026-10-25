package com.example.trimax;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Full-size camera photos for evidence (the camera app writes into a file of ours through the app's
 * FileProvider), instead of the small preview the camera returns by default. The photo is read back
 * scaled down to a sensible size and turned upright.
 */
final class CameraCapture {

    /** Long side of the photo kept as evidence: sharp enough to read a face or a house number. */
    static final int MAX_SIDE = 1600;

    private CameraCapture() {}

    /** An empty file the camera app may write into (in Pictures, covered by res/xml/file_paths.xml). */
    static File newTargetFile(Context context, String prefix) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir == null) return null;
        if (!dir.exists() && !dir.mkdirs()) return null;
        String safe = prefix == null ? "IMG" : prefix.replaceAll("[^A-Za-z0-9]", "_");
        return new File(dir, "CAPTURE_" + safe + "_" + System.currentTimeMillis() + ".jpg");
    }

    /** The camera intent; with a target file, the camera app writes the full-size photo into it. */
    static Intent intent(Context context, File target) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (target == null) return intent;
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", target);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.setClipData(ClipData.newRawUri("", uri));   // lets the camera app write on every API level
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (IllegalArgumentException e) {
            // path not covered by the provider: the camera returns its small preview instead
        }
        return intent;
    }

    /**
     * Reads the photo the camera wrote, scaled so its long side is at most {@link #MAX_SIDE} and turned
     * upright from its EXIF orientation. Null when there is no usable photo in the file.
     */
    static Bitmap readUpright(File file) {
        if (file == null || !file.isFile() || file.length() == 0) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE);
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        } catch (OutOfMemoryError e) {
            opts.inSampleSize *= 2;
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        }
        if (bitmap == null) return null;

        int longSide = Math.max(bitmap.getWidth(), bitmap.getHeight());
        float scale = longSide > MAX_SIDE ? MAX_SIDE / (float) longSide : 1f;
        int degrees = rotation(file);
        if (scale == 1f && degrees == 0) return bitmap;
        Matrix m = new Matrix();
        m.postScale(scale, scale);
        m.postRotate(degrees);
        Bitmap upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
        if (upright != bitmap) bitmap.recycle();
        return upright;
    }

    /** Largest power of two that keeps the long side at or above maxSide (BitmapFactory only halves). */
    static int sampleSize(int width, int height, int maxSide) {
        int sample = 1;
        int longSide = Math.max(width, height);
        while (longSide / (sample * 2) >= maxSide) sample *= 2;
        return sample;
    }

    private static int rotation(File file) {
        try {
            int o = new ExifInterface(file.getAbsolutePath())
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (o == ExifInterface.ORIENTATION_ROTATE_90) return 90;
            if (o == ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (o == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        } catch (Exception ignored) {
            // no EXIF: keep as is
        }
        return 0;
    }
}
