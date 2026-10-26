package com.example.trimax;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.UUID;

/**
 * Customer photos on this phone: one JPEG per customer (by phone number, like the server's matching),
 * in the app's private files, so the list shows them offline. The copy on the server is in the private
 * "customer-photos" bucket, in this agent's own folder; the Admin opens it on the Desktop.
 */
final class CustomerPhotos {

    /** Long side of the photo that is uploaded: enough to recognise a face or a storefront. */
    static final int MAX_SIDE = 1024;
    /** The server's bucket takes JPEGs up to 5 MB; stay well under it. */
    static final int MAX_BYTES = 4_500_000;
    /** Pictures picked from the gallery larger than this are refused before decoding. */
    static final long MAX_PICK_BYTES = 20_000_000L;

    private CustomerPhotos() {}

    /** Same customer = same last 10 phone digits (0917... and +63917... match), as on the server. */
    static String key(String phone) {
        String d = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        if (d.length() < 7) return null;
        return d.length() > 10 ? d.substring(d.length() - 10) : d;
    }

    static File file(Context context, String phone) {
        String k = key(phone);
        if (k == null) return null;
        File dir = new File(context.getFilesDir(), "customer_photos");
        if (!dir.exists() && !dir.mkdirs()) return null;
        return new File(dir, k + ".jpg");
    }

    static boolean has(Context context, String phone) {
        File f = file(context, phone);
        return f != null && f.isFile() && f.length() > 0;
    }

    static void save(Context context, String phone, byte[] jpeg) {
        File f = file(context, phone);
        if (f == null || jpeg == null || jpeg.length == 0) return;
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(jpeg);
        } catch (Exception ignored) {
            // only a local copy; the server has the photo
        }
    }

    /** The customer's photo scaled to at most maxSide, or null. */
    static Bitmap load(Context context, String phone, int maxSide) {
        File f = file(context, phone);
        if (f == null || !f.isFile() || f.length() == 0) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), bounds);
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = CameraCapture.sampleSize(bounds.outWidth, bounds.outHeight, maxSide);
        try {
            return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    /** A JPEG of the photo for upload: long side at most MAX_SIDE, smaller quality until it fits. */
    static byte[] jpeg(Bitmap photo) {
        if (photo == null) return null;
        Bitmap b = photo;
        int longSide = Math.max(photo.getWidth(), photo.getHeight());
        if (longSide > MAX_SIDE) {
            float s = MAX_SIDE / (float) longSide;
            b = Bitmap.createScaledBitmap(photo, Math.max(1, Math.round(photo.getWidth() * s)),
                    Math.max(1, Math.round(photo.getHeight() * s)), true);
        }
        for (int quality = 85; quality >= 50; quality -= 10) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, quality, out);
            if (out.size() > 0 && out.size() <= MAX_BYTES) return out.toByteArray();
        }
        return null;
    }

    /** customer_1729850000000_ab12cd34.jpg: matches the server's rule customer_[A-Za-z0-9_-]{1,80}.jpg */
    static String objectName(long millis, String uuid) {
        String tail = uuid == null ? "x" : uuid.replaceAll("[^A-Za-z0-9]", "");
        if (tail.length() > 8) tail = tail.substring(0, 8);
        return String.format(Locale.US, "customer_%d_%s.jpg", millis, tail);
    }

    /** Gallery pictures this screen accepts (decoded on the phone and re-saved as JPEG for upload). */
    static boolean acceptedPickType(String mimeType) {
        if (mimeType == null) return false;
        String t = mimeType.trim().toLowerCase(Locale.ROOT);
        return t.equals("image/jpeg") || t.equals("image/jpg") || t.equals("image/png")
                || t.equals("image/webp") || t.equals("image/heic") || t.equals("image/heif");
    }

    static String newObjectName() {
        return objectName(System.currentTimeMillis(), UUID.randomUUID().toString());
    }
}
