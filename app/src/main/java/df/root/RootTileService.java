package df.root;

import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Root from the shade.
 *
 * Root here is ephemeral on purpose - the module, su and the daemon are gone the
 * moment the device reboots - so after every reboot the app is two taps away.
 * The tile makes it one, and it does the whole run headlessly: ExploitRunner.run
 * needs a device-protected Context and an IReporter and nothing else, so no
 * activity has to come to the foreground and nothing has to be resumed.
 *
 * Four states, all derived from the device rather than stored:
 *   - /dev/df exists       -> ACTIVE, "Driver active". Tapping opens the app, which is
 *                             the only place that can say what state the device
 *                             is in.
 *   - a run is in flight   -> UNAVAILABLE, "Rooting". Also covers a run started
 *                             from the app, which holds ExploitRunner's own
 *                             monitor and would otherwise make this tap queue
 *                             behind it and then land on a hooked device.
 *   - last run failed      -> UNAVAILABLE, "Reboot". The patch is page-cache
 *                             only, so a retry without a reboot fails the same
 *                             way - the same rule the app's Run pill follows.
 *   - no SU manager picked -> INACTIVE, "Root" with the reason in the subtitle.
 *                             Tappable on purpose: the app is where a manager
 *                             gets picked.
 *
 * The label is recomputed in onStartListening, the only moment the tile is
 * guaranteed to be on screen - a state set while the shade is closed has nowhere
 * to land, because getQsTile() returns null then.
 */
public class RootTileService extends TileService implements IReporter {
    private static final String TAG = "dfroot";
    private static final String PREFS = "dfroot";
    /** A run started from here. Static, because SystemUI destroys the tile
     *  instance as soon as the shade closes - and the run outlives the shade. */
    private static volatile boolean sRunning;

    private final Handler mMain = new Handler(Looper.getMainLooper());
    private final StringBuilder mLog = new StringBuilder();

    @Override
    public void report(String msg) {
        Log.i(TAG, msg.trim());
        mLog.append(msg.trim()).append('\n');
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        paint();
    }

    /** Everything the tile says about the device, from the device. */
    private void paint() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;                 // shade closed; onStartListening will repaint
        }
        if (new File("/dev/df").exists()) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel("Driver active");
            tile.setSubtitle("Open FragX to verify root");
        } else if (sRunning) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setLabel("Rooting\u2026");
            tile.setSubtitle(null);
        } else if (failedThisBoot()) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setLabel("Reboot");
            tile.setSubtitle("Patch is page-cache only");
        } else if (manager() == null) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel("Root");
            tile.setSubtitle("No SU manager");
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel("Root");
            tile.setSubtitle(null);
        }
        tile.updateTile();
    }

    private String manager() {
        return createDeviceProtectedStorageContext()
                .getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString("su_manager", null);
    }

    private int bootCount() {
        try {
            return Settings.Global.getInt(getContentResolver(),
                    Settings.Global.BOOT_COUNT, -1);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * The last run of this boot failed. The log is stamped with the boot it
     * belongs to, so a log from an earlier power session cannot lock the tile -
     * the same invalidation MainActivity does when it opens.
     */
    private boolean failedThisBoot() {
        Context deCtx = createDeviceProtectedStorageContext();
        SharedPreferences sp = deCtx.getSharedPreferences(PREFS, MODE_PRIVATE);
        if (sp.getInt("log_boot_count", -1) != bootCount()) return false;
        if (sp.getBoolean("last_run_success", false)) return false;
        File log = new File(deCtx.getFilesDir(), "last_run.log");
        if (!log.exists()) return false;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(log), StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) {
                if (line.contains("=== exploit failed")) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public void onClick() {
        super.onClick();
        if (sRunning) {
            return;
        }
        if (new File("/dev/df").exists() || manager() == null) {
            openApp();
            return;
        }
        sRunning = true;
        paint();
        start();
    }

    /**
     * The run itself, off the main thread: onClick() has to return immediately
     * and a run is up to 70 seconds. The wake lock is the same one BootReceiver
     * takes - the screen can go off mid-run and the exploit window is the one
     * place it must not.
     */
    private void start() {
        final Context deCtx = createDeviceProtectedStorageContext();
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm == null ? null
                : pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dfroot:tile");
        if (wl != null) {
            wl.acquire(180_000L);
        }
        mLog.setLength(0);
        new Thread(() -> {
            int rc = -1;
            try {
                rc = ExploitRunner.run(deCtx, this);
                Log.i(TAG, "tile: exploit rc=" + rc);
                if (rc != 0) {
                    // exp.c prints no result line of its own; MainActivity
                    // synthesizes this one from the exit code, and its failure UI
                    // and this tile's failedThisBoot() both read it back.
                    String why = rc == 1 ? "ksud nonzero exit"
                            : rc == 2 ? "check logcat & dmesg"
                            : "failed to patch files";
                    report("\n=== exploit failed: " + why + " ===\n");
                }
            } catch (Exception e) {
                Log.e(TAG, "tile: exploit exception", e);
            } finally {
                if (wl != null) {
                    wl.release();
                }
                // A run started from the app can still have been in flight: this
                // one then blocked on ExploitRunner's monitor and came back with
                // "already hooked". Its log is a banner and one line, so leave the
                // app's fuller one where it is.
                if (!mLog.toString().contains("already hooked")) {
                    writeRunLog(deCtx, rc == 0);
                }
                sRunning = false;
                mMain.post(this::paint);
            }
        }, "dfroot-tile").start();
    }

    /**
     * The same last_run.log and the same prefs MainActivity reads on start, so a
     * root from the shade is not invisible when the app is next opened. The
     * activity throws this log away when log_boot_count is not the current boot,
     * so stamp it - otherwise the tile's log would be deleted as stale the first
     * time the app is opened after it.
     */
    private void writeRunLog(Context deCtx, boolean ok) {
        try (FileOutputStream out =
                     new FileOutputStream(new File(deCtx.getFilesDir(), "last_run.log"))) {
            out.write(mLog.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "tile: writing the run log failed", e);
        }
        deCtx.getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("last_run_success", ok)
                .putInt("log_boot_count", bootCount())
                .apply();
    }

    /** The app, collapsed onto whatever is in front. API 34 moved this to a
     *  PendingIntent; the Intent overload still exists there but is deprecated. */
    private void openApp() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        } else {
            startActivityAndCollapse(i);
        }
    }

    /**
     * Offer the tile in the shade, once. Android 13+ only: below that the tile is
     * still there, it just has to be dragged in from the edit screen by hand.
     * Called from MainActivity, because a tile that was never added is never
     * instantiated and so can never offer itself.
     */
    static void offer(Context ctx) {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        Context deCtx = ctx.createDeviceProtectedStorageContext();
        if (deCtx.getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean("tile_offered", false)) {
            return;
        }
        StatusBarManager sbm = ctx.getSystemService(StatusBarManager.class);
        if (sbm == null) {
            return;
        }
        sbm.requestAddTileService(
                new ComponentName(ctx, RootTileService.class),
                "FragX",
                Icon.createWithResource(ctx, R.drawable.ic_tile_root),
                ctx.getMainExecutor(),
                result -> Log.i(TAG, "tile request: " + result));
        deCtx.getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putBoolean("tile_offered", true).apply();
    }
}
