package com.example.trimax;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Adds a customer. A new customer needs a photo (camera or gallery): it is uploaded to the private
 * customer-photos storage and the customer is saved on the TRIMAX server, so the Admin sees both on the
 * Desktop. A copy of the photo and the customer stay on this phone for the customer list.
 *
 * Extras in (all optional, to prefill): CUSTOMER_NAME, CUSTOMER_PHONE, CUSTOMER_EMAIL, CUSTOMER_ADDRESS,
 * CUSTOMER_LAT, CUSTOMER_LNG, and EXTRA_NOTICE (a line shown on top, e.g. why the agent was sent here).
 * Result: RESULT_OK with the same CUSTOMER_* extras.
 */
public class AddCustomerActivity extends AppCompatActivity {

    public static final String EXTRA_NOTICE = "ADD_CUSTOMER_NOTICE";

    private static final String STATE_ADDRESS = "add_customer_address";
    private static final String STATE_LAT = "add_customer_lat";
    private static final String STATE_LNG = "add_customer_lng";
    private static final String STATE_CAPTURE = "add_customer_capture";

    private TextInputEditText etName, etPhone, etEmail;
    private TextView tvSelectedLocation, tvPhotoStatus, tvError;
    private ImageView ivPhoto;
    private ProgressBar progress;
    private MaterialButton btnTakePhoto, btnPickPhoto, btnSave, btnChooseLocation;
    private String selectedAddress = "";
    private double selectedLatitude = 0, selectedLongitude = 0;
    private File pendingCapture;
    private boolean busy;
    private boolean photoWorking;

    private ActivityResultLauncher<Intent> locationPickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_customer);

        View root = findViewById(R.id.addCustomerRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        etName = findViewById(R.id.etAddCustomerName);
        etPhone = findViewById(R.id.etAddCustomerPhone);
        etEmail = findViewById(R.id.etAddCustomerEmail);
        tvSelectedLocation = findViewById(R.id.tvAddCustomerLocation);
        tvPhotoStatus = findViewById(R.id.tvCustomerPhotoStatus);
        tvError = findViewById(R.id.tvAddCustomerError);
        ivPhoto = findViewById(R.id.ivCustomerPhoto);
        progress = findViewById(R.id.progressAddCustomer);
        btnTakePhoto = findViewById(R.id.btnTakeCustomerPhoto);
        btnPickPhoto = findViewById(R.id.btnPickCustomerPhoto);
        btnSave = findViewById(R.id.btnSaveNewCustomer);
        btnChooseLocation = findViewById(R.id.btnChooseCustomerLocation);

        Intent in = getIntent();
        if (savedInstanceState == null) {
            draftFile().delete();   // a photo left from an earlier, unfinished customer
            setText(etName, in.getStringExtra("CUSTOMER_NAME"));
            setText(etPhone, in.getStringExtra("CUSTOMER_PHONE"));
            setText(etEmail, in.getStringExtra("CUSTOMER_EMAIL"));
            String address = in.getStringExtra("CUSTOMER_ADDRESS");
            double lat = in.getDoubleExtra("CUSTOMER_LAT", 0), lng = in.getDoubleExtra("CUSTOMER_LNG", 0);
            if (address != null && !address.trim().isEmpty() && validPin(lat, lng)) {
                selectedAddress = address.trim();
                selectedLatitude = lat;
                selectedLongitude = lng;
            }
        } else {
            selectedAddress = savedInstanceState.getString(STATE_ADDRESS, "");
            selectedLatitude = savedInstanceState.getDouble(STATE_LAT, 0);
            selectedLongitude = savedInstanceState.getDouble(STATE_LNG, 0);
            String capture = savedInstanceState.getString(STATE_CAPTURE);
            pendingCapture = capture != null ? new File(capture) : null;
        }
        showLocation();
        showPhoto();

        String notice = in.getStringExtra(EXTRA_NOTICE);
        TextView tvNotice = findViewById(R.id.tvAddCustomerNotice);
        if (tvNotice != null && notice != null && !notice.trim().isEmpty()) {
            tvNotice.setText(notice.trim());
            tvNotice.setVisibility(View.VISIBLE);
        }

        locationPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    Intent data = result.getData();
                    selectedAddress = data.getStringExtra("CUSTOMER_ADDRESS");
                    if (selectedAddress == null) selectedAddress = "";
                    selectedLatitude = data.getDoubleExtra("CUSTOMER_LAT", 0);
                    selectedLongitude = data.getDoubleExtra("CUSTOMER_LNG", 0);
                    showLocation();
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    File raw = pendingCapture;
                    pendingCapture = null;
                    if (result.getResultCode() != RESULT_OK) {
                        if (raw != null) raw.delete();
                        return;
                    }
                    Bitmap preview = null;
                    if (result.getData() != null && result.getData().getExtras() != null) {
                        Object data = result.getData().getExtras().get("data");
                        if (data instanceof Bitmap) preview = (Bitmap) data;
                    }
                    final Bitmap small = preview;
                    processPhoto(() -> {
                        // Full-size photo from the file the camera wrote; the camera's small preview otherwise.
                        Bitmap photo = CameraCapture.readUpright(raw);
                        if (raw != null) raw.delete();
                        return photo != null ? photo : small;
                    });
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        launchCamera();
                    } else {
                        showError("Camera permission is needed to take the customer's photo. "
                                + "You can choose a picture from the gallery instead.");
                    }
                });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) onPicked(uri);
        });

        View btnBack = findViewById(R.id.btnAddCustomerBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnChooseLocation != null) btnChooseLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, NewBookingLocationActivity.class);
            intent.putExtra("CUSTOMER_LOCATION_PICKER_ONLY", true);
            intent.putExtra("CUSTOMER_NAME", textOf(etName));
            intent.putExtra("CUSTOMER_PHONE", textOf(etPhone));
            intent.putExtra("CUSTOMER_EMAIL", textOf(etEmail));
            intent.putExtra("CUSTOMER_ADDRESS", selectedAddress);
            intent.putExtra("CUSTOMER_LAT", selectedLatitude);
            intent.putExtra("CUSTOMER_LNG", selectedLongitude);
            locationPickerLauncher.launch(intent);
        });
        if (btnTakePhoto != null) btnTakePhoto.setOnClickListener(v -> takePhoto());
        if (btnPickPhoto != null) btnPickPhoto.setOnClickListener(v -> {
            if (busy || photoWorking) return;
            try {
                galleryLauncher.launch("image/*");
            } catch (Exception e) {
                showError("No gallery app is available on this phone. Take a photo instead.");
            }
        });
        if (btnSave != null) btnSave.setOnClickListener(v -> saveCustomer());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ADDRESS, selectedAddress);
        outState.putDouble(STATE_LAT, selectedLatitude);
        outState.putDouble(STATE_LNG, selectedLongitude);
        if (pendingCapture != null) outState.putString(STATE_CAPTURE, pendingCapture.getAbsolutePath());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.shutdown();
        if (isFinishing()) draftFile().delete();
    }

    // ------------------------------------------------------------------ photo

    private void takePhoto() {
        if (busy || photoWorking) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        pendingCapture = CameraCapture.newTargetFile(this, "CUSTOMER");
        try {
            cameraLauncher.launch(CameraCapture.intent(this, pendingCapture));
        } catch (Exception e) {
            if (pendingCapture != null) pendingCapture.delete();
            pendingCapture = null;
            showError("No camera app is available on this phone. Choose a picture from the gallery instead.");
        }
    }

    private void onPicked(Uri uri) {
        String type = null;
        try {
            type = getContentResolver().getType(uri);
        } catch (Exception ignored) {
            // checked below
        }
        if (!CustomerPhotos.acceptedPickType(type)) {
            showError("Choose a JPG, PNG, WEBP or HEIC picture.");
            return;
        }
        long size = sizeOf(uri);
        if (size > CustomerPhotos.MAX_PICK_BYTES) {
            showError(String.format(Locale.US, "That picture is too large (%.1f MB). Choose one of 20 MB or less.",
                    size / 1_000_000.0));
            return;
        }
        processPhoto(() -> {
            File tmp = new File(getCacheDir(), "customer_pick.tmp");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(tmp)) {
                if (in == null) return null;
                byte[] buf = new byte[64 * 1024];
                long total = 0;
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > CustomerPhotos.MAX_PICK_BYTES) throw new TooLarge();
                    out.write(buf, 0, n);
                }
            }
            try {
                return CameraCapture.readUpright(tmp);
            } finally {
                tmp.delete();
            }
        });
    }

    private interface PhotoSource {
        Bitmap load() throws Exception;
    }

    private static final class TooLarge extends Exception {}

    /** Turns the photo into the JPEG that will be uploaded (off the main thread) and shows it. */
    private void processPhoto(PhotoSource source) {
        setPhotoWorking(true);
        worker.execute(() -> {
            String problem = null;
            try {
                Bitmap photo = source.load();
                byte[] jpeg = photo == null ? null : CustomerPhotos.jpeg(photo);
                if (jpeg == null) {
                    problem = "This picture could not be read. Take a new photo or choose another picture.";
                } else {
                    try (OutputStream out = new FileOutputStream(draftFile())) {
                        out.write(jpeg);
                    }
                }
            } catch (TooLarge e) {
                problem = "That picture is too large. Choose one of 20 MB or less.";
            } catch (OutOfMemoryError | Exception e) {
                problem = "This picture could not be read. Take a new photo or choose another picture.";
            }
            final String error = problem;
            runOnUiThread(() -> {
                if (SessionGuard.isGone(this)) return;
                setPhotoWorking(false);
                showPhoto();
                if (error != null) showError(error);
                else hideError();
            });
        });
    }

    private void setPhotoWorking(boolean working) {
        photoWorking = working;
        if (progress != null) progress.setVisibility(working || busy ? View.VISIBLE : View.GONE);
        if (working && tvPhotoStatus != null) tvPhotoStatus.setText("Preparing the photo...");
        enableButtons();
    }

    /** The JPEG waiting to be uploaded; in the cache, so it survives screen rotation. */
    private File draftFile() {
        return new File(getCacheDir(), "customer_photo_draft.jpg");
    }

    private boolean hasDraft() {
        File f = draftFile();
        return f.isFile() && f.length() > 0;
    }

    private void showPhoto() {
        if (ivPhoto == null) return;
        Bitmap b = null;
        if (hasDraft()) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2;   // the preview is small; the upload keeps the full JPEG
            try {
                b = BitmapFactory.decodeFile(draftFile().getAbsolutePath(), o);
            } catch (OutOfMemoryError ignored) {
                b = null;
            }
        }
        if (b != null) {
            ivPhoto.setPadding(0, 0, 0, 0);
            ImageViewCompat.setImageTintList(ivPhoto, null);
            ivPhoto.setImageBitmap(b);
            if (tvPhotoStatus != null) tvPhotoStatus.setText("Photo ready. Retake it if the face or store front is not clear.");
            if (btnTakePhoto != null) btnTakePhoto.setText("Retake");
        } else {
            int pad = Math.round(24 * getResources().getDisplayMetrics().density);
            ivPhoto.setPadding(pad, pad, pad, pad);
            ImageViewCompat.setImageTintList(ivPhoto,
                    ContextCompat.getColorStateList(this, R.color.text_secondary));
            ivPhoto.setImageResource(R.drawable.ic_user);
            if (tvPhotoStatus != null) {
                tvPhotoStatus.setText("Take a photo of the customer or the store front. It is required for a new customer.");
            }
            if (btnTakePhoto != null) btnTakePhoto.setText("Take photo");
        }
    }

    private long sizeOf(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getLong(0);
        } catch (Exception ignored) {
            // unknown: the copy above stops at the limit
        }
        return -1;
    }

    // ------------------------------------------------------------------ save

    private void saveCustomer() {
        if (busy || photoWorking) return;
        hideError();
        String name = textOf(etName);
        String phone = textOf(etPhone);
        String email = textOf(etEmail);
        String cleanPhone = phone.replaceAll("[^0-9]", "");

        if (name.isEmpty() || name.length() > 100) {
            etName.setError(name.isEmpty() ? "Customer name is required" : "Use 100 characters or fewer");
            etName.requestFocus();
            return;
        }
        if (phone.isEmpty() || cleanPhone.length() < 7 || phone.length() > 30) {
            etPhone.setError("Enter a valid phone number");
            etPhone.requestFocus();
            return;
        }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (selectedAddress.trim().isEmpty() || !validPin(selectedLatitude, selectedLongitude)) {
            showError("Choose the customer's location first.");
            return;
        }
        if (!hasDraft()) {
            showError("Take or choose a photo of the customer before saving.");
            return;
        }

        setBusy(true);
        final String address = selectedAddress.trim();
        final double lat = selectedLatitude, lng = selectedLongitude;
        worker.execute(() -> {
            byte[] jpeg = readDraft();
            runOnUiThread(() -> {
                if (SessionGuard.isGone(this)) return;
                if (jpeg == null) {
                    setBusy(false);
                    showPhoto();
                    showError("The photo could not be read. Take it again.");
                    return;
                }
                TrimaxApi.get(this).saveCustomer(name, phone, address, lat, lng, jpeg,
                        new TrimaxApi.Callback<ServerCustomer>() {
                            @Override
                            public void onSuccess(ServerCustomer saved) {
                                if (SessionGuard.isGone(AddCustomerActivity.this)) return;
                                onSaved(saved, name, phone, email, address, lat, lng, jpeg);
                            }

                            @Override
                            public void onError(ApiException error) {
                                if (SessionGuard.isGone(AddCustomerActivity.this)) return;
                                setBusy(false);
                                if (SessionGuard.handle(AddCustomerActivity.this, error)) return;
                                String message = error.getMessage();
                                if (error.getKind() == ApiException.Kind.NETWORK
                                        || error.getKind() == ApiException.Kind.TIMEOUT) {
                                    message += " Your details and photo are kept: tap Retry save.";
                                }
                                showError(message);
                                if (btnSave != null) btnSave.setText("Retry save");
                            }
                        });
            });
        });
    }

    private void onSaved(ServerCustomer saved, String name, String phone, String email, String address,
                         double lat, double lng, byte[] jpeg) {
        String savedName = saved != null && saved.name != null ? saved.name : name;
        String savedPhone = saved != null && saved.contact != null ? saved.contact : phone;
        worker.execute(() -> {
            // Local conveniences: the photo and the customer on this phone, for the list and new bookings.
            CustomerPhotos.save(getApplicationContext(), savedPhone, jpeg);
            boolean local = new DatabaseHelper(getApplicationContext())
                    .saveCustomer(savedName, savedPhone, email, address, lat, lng);
            runOnUiThread(() -> {
                if (SessionGuard.isGone(this)) return;
                draftFile().delete();
                Intent result = new Intent();
                result.putExtra("CUSTOMER_NAME", savedName);
                result.putExtra("CUSTOMER_PHONE", savedPhone);
                result.putExtra("CUSTOMER_EMAIL", email);
                result.putExtra("CUSTOMER_ADDRESS", address);
                result.putExtra("CUSTOMER_LAT", lat);
                result.putExtra("CUSTOMER_LNG", lng);
                setResult(RESULT_OK, result);
                Toast.makeText(this, local ? "Customer saved." : "Customer saved on the server.", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private byte[] readDraft() {
        File f = draftFile();
        if (!f.isFile() || f.length() <= 0 || f.length() > CustomerPhotos.MAX_BYTES) return null;
        byte[] data = new byte[(int) f.length()];
        try (FileInputStream in = new FileInputStream(f)) {
            int off = 0;
            while (off < data.length) {
                int n = in.read(data, off, data.length - off);
                if (n < 0) return null;
                off += n;
            }
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ screen state

    private void setBusy(boolean b) {
        busy = b;
        if (progress != null) progress.setVisibility(b || photoWorking ? View.VISIBLE : View.GONE);
        if (btnSave != null && b) btnSave.setText("Saving...");
        if (btnSave != null && !b) btnSave.setText("Save Customer");
        enableButtons();
    }

    private void enableButtons() {
        boolean on = !busy && !photoWorking;
        if (btnSave != null) btnSave.setEnabled(on);
        if (btnTakePhoto != null) btnTakePhoto.setEnabled(on);
        if (btnPickPhoto != null) btnPickPhoto.setEnabled(on);
        if (btnChooseLocation != null) btnChooseLocation.setEnabled(!busy);
    }

    private void showLocation() {
        if (tvSelectedLocation != null) {
            tvSelectedLocation.setText(selectedAddress.isEmpty() ? "No location selected" : selectedAddress);
        }
    }

    private void showError(String message) {
        if (tvError != null) {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void hideError() {
        if (tvError != null) tvError.setVisibility(View.GONE);
    }

    private static boolean validPin(double lat, double lng) {
        return Double.isFinite(lat) && Double.isFinite(lng) && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180 && !(lat == 0 && lng == 0);
    }

    private static void setText(TextInputEditText field, String value) {
        if (field != null && value != null && !value.trim().isEmpty()) field.setText(value.trim());
    }

    private String textOf(TextInputEditText field) {
        return field != null && field.getText() != null ? field.getText().toString().trim() : "";
    }
}
