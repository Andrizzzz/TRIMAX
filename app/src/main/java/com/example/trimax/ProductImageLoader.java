package com.example.trimax;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Product pictures from the public "product-images" bucket (uploaded by the Desktop admin).
 *
 * Loads in the background, keeps decoded pictures in memory and the downloaded files on disk (a picture
 * never changes at its address: a replaced picture gets a new address), shows the placeholder while
 * loading and for products without a picture, missing files, bad addresses or no connection, and
 * never puts a picture into a row that was meanwhile reused for another product.
 */
public final class ProductImageLoader {

    private static final int MAX_BYTES = 3 * 1024 * 1024;
    private static final long DISK_LIMIT = 40L * 1024 * 1024;

    private static ProductImageLoader instance;

    private final File diskDir;
    private final LruCache<String, Bitmap> memory;
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());
    /** Addresses that failed in this run (404, not a picture): not retried until the list is reloaded. */
    private final Set<String> failed = Collections.synchronizedSet(new HashSet<>());

    private ProductImageLoader(Context context) {
        diskDir = new File(context.getApplicationContext().getCacheDir(), "product-images");
        int kb = (int) (Runtime.getRuntime().maxMemory() / 1024 / 16);   // 1/16 of the app's memory
        memory = new LruCache<String, Bitmap>(Math.max(kb, 4 * 1024)) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    public static synchronized ProductImageLoader get(Context context) {
        if (instance == null) instance = new ProductImageLoader(context);
        return instance;
    }

    /** Forget failures so a refresh tries the missing pictures again. */
    public void retryFailed() {
        failed.clear();
    }

    /**
     * Shows the picture at url in the view (placeholder first). targetPx is the largest side the view
     * shows, used to decode a small enough bitmap.
     */
    public void load(ImageView view, String url, int placeholderRes, int targetPx) {
        view.setTag(R.id.tag_product_image_url, url);
        if (url == null) {
            showPlaceholder(view, placeholderRes);
            return;
        }
        String key = url + "@" + targetPx;
        Bitmap cached = memory.get(key);
        if (cached != null) {
            showPicture(view, cached);
            return;
        }
        showPlaceholder(view, placeholderRes);
        if (failed.contains(url)) return;
        pool.execute(() -> {
            Bitmap bmp = null;
            try {
                byte[] bytes = readDisk(url);
                if (bytes == null) {
                    bytes = download(url);
                    if (bytes != null) writeDisk(url, bytes);
                }
                if (bytes != null) bmp = decode(bytes, targetPx);
            } catch (Exception ignored) {
                // offline or server trouble: placeholder stays; tried again next time the screen loads
            }
            final Bitmap result = bmp;
            if (result != null) memory.put(key, result);
            main.post(() -> {
                if (url.equals(view.getTag(R.id.tag_product_image_url)) && result != null) {
                    showPicture(view, result);
                }
            });
        });
    }

    /** Photos fill their box; the placeholder icon keeps its own proportions. */
    private static void showPicture(ImageView view, Bitmap bmp) {
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setImageBitmap(bmp);
    }

    private static void showPlaceholder(ImageView view, int placeholderRes) {
        view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        view.setImageResource(placeholderRes);
    }

    private byte[] download(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(10_000);
            c.setReadTimeout(20_000);
            c.setInstanceFollowRedirects(false);
            int status = c.getResponseCode();
            if (status == 400 || status == 404) {   // the picture is not on the server (any more)
                failed.add(url);
                return null;
            }
            if (status != 200) return null;          // server trouble: try again later
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    if (out.size() > MAX_BYTES) {
                        failed.add(url);
                        return null;
                    }
                }
                return out.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }

    private Bitmap decode(byte[] bytes, int targetPx) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = CameraCapture.sampleSize(bounds.outWidth, bounds.outHeight, Math.max(64, targetPx));
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
    }

    // ------------------------------------------------------------------ disk cache

    private File fileFor(String url) {
        // The address ends with products/<id>/<random>.jpg, which is already unique and file-name safe.
        String tail = url.substring(url.indexOf("products/") + "products/".length()).replace('/', '_');
        return new File(diskDir, tail);
    }

    private byte[] readDisk(String url) {
        File f = fileFor(url);
        if (!f.isFile() || f.length() == 0 || f.length() > MAX_BYTES) return null;
        //noinspection ResultOfMethodCallIgnored
        f.setLastModified(System.currentTimeMillis());
        return DutySync.readJpeg(f, null);
    }

    private synchronized void writeDisk(String url, byte[] bytes) {
        try {
            if (!diskDir.isDirectory() && !diskDir.mkdirs()) return;
            File tmp = new File(diskDir, "tmp_" + System.nanoTime());
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(bytes);
            }
            //noinspection ResultOfMethodCallIgnored
            tmp.renameTo(fileFor(url));
            trimDisk();
        } catch (Exception ignored) {
            // no space: pictures are simply downloaded again next time
        }
    }

    /** Keeps the disk cache under its limit, removing the least recently used pictures first. */
    private void trimDisk() {
        File[] files = diskDir.listFiles();
        if (files == null) return;
        long total = 0;
        for (File f : files) total += f.length();
        if (total <= DISK_LIMIT) return;
        java.util.Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File f : files) {
            if (total <= DISK_LIMIT * 3 / 4) break;
            total -= f.length();
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}
