package df.root;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import java.text.DateFormat;
import java.util.Date;

/** Durable bookkeeping lives outside the native bootstrap's size-limited dfroot.xml. */
final class RecoveryState {
    static SharedPreferences prefs(Context c) {
        return c.createDeviceProtectedStorageContext().getSharedPreferences("fragx_recovery", Context.MODE_PRIVATE);
    }
    private static SharedPreferences rootPrefs(Context c) {
        return c.createDeviceProtectedStorageContext().getSharedPreferences("dfroot", Context.MODE_PRIVATE);
    }
    static int boot(Context c) {
        return Settings.Global.getInt(c.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
    }
    static boolean alreadyAttemptedThisBoot(Context c) {
        return boot(c) >= 0 && prefs(c).getInt("autorun_boot", -2) == boot(c);
    }
    static synchronized boolean allowAutorun(Context c) {
        SharedPreferences p = prefs(c);
        int boot = boot(c);
        if (!RecoveryPolicy.mayAutorun(p.getBoolean("pending", false), p.getBoolean("failed", false),
                boot, p.getInt("autorun_boot", -2))) return false;
        // Commit before returning, so a killed process cannot silently retry.
        return p.edit().putInt("autorun_boot", boot).commit();
    }
    static synchronized void rearm(Context c) {
        prefs(c).edit().putBoolean("pending", false).putBoolean("failed", false)
                .remove("autorun_boot").commit();
    }
    static synchronized void begin(Context c) {
        restoreModulePreference(c);
        SharedPreferences p = prefs(c);
        if (!p.edit().putBoolean("pending", true).putLong("attempt_at", System.currentTimeMillis())
                .putInt("attempt_boot", boot(c)).putString("result", "Attempt started; completion unconfirmed").commit())
            throw new IllegalStateException("Cannot persist recovery interlock");
        if (p.getBoolean("without_modules_once", false)) {
            boolean original = rootPrefs(c).getBoolean("disable_modules", false);
            if (!p.edit().putBoolean("restore_modules", true).putBoolean("original_disable", original)
                    .putBoolean("without_modules_once", false).commit())
                throw new IllegalStateException("Cannot persist module preference backup");
            if (!rootPrefs(c).edit().putBoolean("disable_modules", true).commit())
                throw new IllegalStateException("Cannot stage module-free attempt");
        }
    }
    static synchronized void restoreModulePreference(Context c) {
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("restore_modules", false)) return;
        if (rootPrefs(c).edit().putBoolean("disable_modules", p.getBoolean("original_disable", false)).commit())
            p.edit().remove("restore_modules").commit();
    }
    static synchronized void finish(Context c, String result, boolean ok) {
        prefs(c).edit().putBoolean("pending", false).putBoolean("failed", !ok)
                .putString("result", result).putLong("finished_at", System.currentTimeMillis()).commit();
    }
    static synchronized void recordVerifiedRoot(Context c) {
        SharedPreferences p = prefs(c);
        SharedPreferences.Editor edit = p.edit()
                .putLong("root_verified_at", System.currentTimeMillis())
                .putInt("root_verified_boot", boot(c));
        // A later live check can resolve an interrupted attempt in this boot.
        // Never clear a recorded native failure or an attempt still running.
        if (!ExploitRunner.active && p.getBoolean("pending", false)
                && !p.getBoolean("failed", false)
                && boot(c) >= 0 && p.getInt("attempt_boot", -1) == boot(c)) {
            edit.putBoolean("pending", false)
                    .putString("result", "Live UID 0 verified after interrupted attempt");
        }
        edit.apply();
    }
    static String summary(Context c) {
        SharedPreferences p = prefs(c);
        long at = p.getLong("attempt_at", 0);
        StringBuilder s = new StringBuilder("LAST ROOT ATTEMPT\n");
        if (at == 0) s.append("No attempt recorded by this version.\n");
        else {
            s.append("Started: ").append(date(at)).append("\nBoot: ").append(p.getInt("attempt_boot", -1)).append('\n');
            s.append(p.getBoolean("pending", false) ? "Completion unconfirmed (process ended or attempt still running)"
                    : p.getString("result", "Unknown")).append('\n');
        }
        long verified = p.getLong("root_verified_at", 0);
        s.append("Last UID 0 verification: ").append(verified == 0 ? "Not recorded" : date(verified)).append('\n');
        s.append("Autorun protection: pauses after one failed/unconfirmed attempt; re-enable Autorun to retry.\n");
        s.append("Next attempt without modules: ").append(p.getBoolean("without_modules_once", false) ? "Yes" : "No").append('\n');
        s.append("Soft reboot completion: not independently verified.\n");
        long reboot = p.getLong("reboot_at", 0);
        if (reboot > 0) s.append("Last reboot request: ").append(p.getString("reboot_kind", "unknown"))
                .append(" at ").append(date(reboot)).append(" (request only)\n");
        return s.toString();
    }
    private static String date(long at) { return DateFormat.getDateTimeInstance().format(new Date(at)); }
    private RecoveryState() {}
}
