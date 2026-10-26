package com.example.trimax;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shows customer photos in lists and on the customer screen: the copy on this phone first, otherwise
 * the photo on the server (this agent's own uploads), which is then kept on the phone. Customers saved
 * before photos were required keep the placeholder.
 */
final class CustomerAvatars {

    enum State { LOADING, SHOWN, NO_PHOTO, ERROR }

    interface Listener {
        void onState(State state, String message);
    }

    private static final LruCache<String, Bitmap> MEMORY = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    /** Downloads that failed in this run: lists do not retry them on every redraw (the detail screen can). */
    private static final Set<String> FAILED = new HashSet<>();
    private static final ExecutorService DISK = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private CustomerAvatars() {}

    /**
     * Puts the customer's photo into view (scaled to maxSide pixels) or leaves the placeholder.
     * @param photoPath the server path from trimax_agent_list_customers, or null when unknown
     * @param retry true to try a download again that failed before (the detail screen's Retry)
     */
    static void bind(Activity activity, ImageView view, String phone, String photoPath, int maxSide,
                     boolean retry, Listener listener) {
        String key = CustomerPhotos.key(phone);
        if (view == null || key == null) {
            report(listener, State.NO_PHOTO, null);
            return;
        }
        String memKey = key + "@" + maxSide;
        view.setTag(R.id.tag_customer_photo_key, memKey);
        Bitmap cached = MEMORY.get(memKey);
        if (cached != null) {
            show(view, cached);
            report(listener, State.SHOWN, null);
            return;
        }
        boolean onServer = photoPath != null && !photoPath.trim().isEmpty();
        if (!CustomerPhotos.has(activity, phone) && (!onServer || (FAILED.contains(key) && !retry))) {
            report(listener, onServer ? State.ERROR : State.NO_PHOTO,
                    onServer ? "The photo could not be loaded." : null);
            return;
        }
        report(listener, State.LOADING, null);
        final android.content.Context app = activity.getApplicationContext();
        DISK.execute(() -> {
            Bitmap local = CustomerPhotos.load(app, phone, maxSide);
            if (local != null) {
                MEMORY.put(memKey, local);
                MAIN.post(() -> {
                    if (SessionGuard.isGone(activity) || !memKey.equals(view.getTag(R.id.tag_customer_photo_key))) return;
                    show(view, local);
                    report(listener, State.SHOWN, null);
                });
                return;
            }
            if (!onServer) {
                MAIN.post(() -> report(listener, State.NO_PHOTO, null));
                return;
            }
            MAIN.post(() -> TrimaxApi.get(app).downloadCustomerPhoto(photoPath, new TrimaxApi.Callback<byte[]>() {
                @Override
                public void onSuccess(byte[] jpeg) {
                    FAILED.remove(key);
                    DISK.execute(() -> {
                        CustomerPhotos.save(app, phone, jpeg);
                        Bitmap b = decode(jpeg, maxSide);
                        if (b != null) MEMORY.put(memKey, b);
                        MAIN.post(() -> {
                            if (SessionGuard.isGone(activity)) return;
                            if (b == null) {
                                report(listener, State.ERROR, "The photo could not be read.");
                                return;
                            }
                            if (memKey.equals(view.getTag(R.id.tag_customer_photo_key))) show(view, b);
                            report(listener, State.SHOWN, null);
                        });
                    });
                }

                @Override
                public void onError(ApiException error) {
                    FAILED.add(key);
                    if (SessionGuard.isGone(activity)) return;
                    report(listener, State.ERROR, error.getMessage());
                }
            }));
        });
    }

    /** Forget the photo kept in memory (after a new photo was saved for this customer). */
    static void forget(String phone) {
        String key = CustomerPhotos.key(phone);
        if (key == null) return;
        for (String k : MEMORY.snapshot().keySet()) {
            if (k.startsWith(key + "@")) MEMORY.remove(k);
        }
        FAILED.remove(key);
    }

    private static Bitmap decode(byte[] jpeg, int maxSide) {
        if (jpeg == null || jpeg.length == 0) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, bounds);
        if (bounds.outWidth <= 0) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = CameraCapture.sampleSize(bounds.outWidth, bounds.outHeight, maxSide);
        try {
            return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, o);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private static void show(ImageView view, Bitmap b) {
        view.setPadding(0, 0, 0, 0);
        androidx.core.widget.ImageViewCompat.setImageTintList(view, null);
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setClipToOutline(true);
        view.setImageBitmap(b);
    }

    private static void report(Listener listener, State state, String message) {
        if (listener != null) listener.onState(state, message);
    }
}
