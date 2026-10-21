package df.root;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.util.AtomicFile;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Private, bounded wallpaper copies: no storage permission or lasting URI grant. */
final class CustomBackground {
    private final AppCompatActivity activity;
    private final ImageView view;
    private final AtomicFile file;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String> picker;
    private int generation;

    CustomBackground(AppCompatActivity activity, ImageView view) {
        this.activity = activity;
        this.view = view;
        file = new AtomicFile(new File(activity.getFilesDir(), "custom_background.png"));
        picker = activity.registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) importImage(uri); });
        view.setColorFilter(0x66000000, PorterDuff.Mode.SRC_ATOP);
        int request = ++generation;
        worker.execute(() -> {
            if (!file.getBaseFile().exists()) return;
            try {
                // Recover any interrupted atomic save before decoding.
                try (java.io.FileInputStream ignored = file.openRead()) { }
                Bitmap bitmap = decode(ImageDecoder.createSource(file.getBaseFile()));
                show(bitmap, request);
            } catch (IOException | RuntimeException error) {
                // Keep the built-in background if the stored image is unavailable.
            }
        });
    }

    void choose() {
        try {
            picker.launch("image/*");
        } catch (android.content.ActivityNotFoundException error) {
            Toast.makeText(activity, "No image picker is available", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decode(ImageDecoder.Source source) throws IOException {
        return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
            int width = info.getSize().getWidth();
            int height = info.getSize().getHeight();
            float scale = Math.min(1f, 1600f / Math.max(width, height));
            decoder.setTargetSize(Math.max(1, Math.round(width * scale)),
                    Math.max(1, Math.round(height * scale)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
    }

    private void importImage(Uri uri) {
        int request = ++generation;
        worker.execute(() -> {
            FileOutputStream output = null;
            try {
                Bitmap bitmap = decode(ImageDecoder.createSource(activity.getContentResolver(), uri));
                output = file.startWrite();
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                    throw new IOException("Could not save background");
                file.finishWrite(output);
                output = null;
                show(bitmap, request);
            } catch (IOException | RuntimeException error) {
                if (output != null) file.failWrite(output);
                activity.runOnUiThread(() -> {
                    if (isCurrent(request)) Toast.makeText(activity,
                            "Couldn't load that image. Try another photo.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean isCurrent(int request) {
        return request == generation && !activity.isFinishing() && !activity.isDestroyed();
    }

    private void show(Bitmap bitmap, int request) {
        activity.runOnUiThread(() -> {
            if (!isCurrent(request)) { bitmap.recycle(); return; }
            view.setImageBitmap(bitmap);
            view.setVisibility(View.VISIBLE);
        });
    }

    void reset() {
        ++generation;
        view.setImageDrawable(null);
        view.setVisibility(View.GONE);
        worker.execute(file::delete);
    }

    void close() {
        ++generation;
        worker.shutdown();
    }
}
