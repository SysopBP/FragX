package df.root;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.File;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only prerequisites; this does not predict exploit or driver success. */
final class Diagnostics {
    static String snapshot(Context context) {
        StringBuilder out = new StringBuilder("FRAGX DIAGNOSTICS\n");
        out.append("App: ").append(BuildConfig.VERSION_NAME).append(" (")
                .append(BuildConfig.VERSION_CODE).append(")\n");
        out.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        out.append("Android: ").append(Build.VERSION.RELEASE).append(" / API ")
                .append(Build.VERSION.SDK_INT).append('\n');
        out.append("Security patch: ").append(Build.VERSION.SECURITY_PATCH).append('\n');
        out.append("ABIs: ").append(Arrays.toString(Build.SUPPORTED_ABIS)).append('\n');
        String kernel = System.getProperty("os.version", "unknown");
        out.append("Kernel: ").append(kernel).append('\n');
        out.append("KMI hint: ").append(kmiHint(kernel)).append('\n');
        out.append("Root marker /dev/df: ").append(new File("/dev/df").exists() ? "present" : "absent").append('\n');
        int missing = 0;
        String nativeDir = context.getApplicationInfo().nativeLibraryDir;
        for (String name : new String[]{"libdfroot.so", "libbootstrap.so"}) {
            boolean exists = nativeDir != null && new File(nativeDir, name).isFile();
            out.append(name).append(": ").append(exists ? "present" : "MISSING").append('\n');
            if (!exists) missing++;
        }
        String manager = context.createDeviceProtectedStorageContext()
                .getSharedPreferences("dfroot", Context.MODE_PRIVATE).getString("su_manager", null);
        out.append("Selected manager: ").append(manager == null ? "none" : manager).append('\n');
        if (manager == null) {
            out.append("Action: select an SU manager before running.\n");
            missing++;
        } else {
            try {
                ApplicationInfo app = context.getPackageManager().getApplicationInfo(manager, 0);
                boolean daemon = ExploitRunner.hasKsud(app);
                out.append("Manager libksud.so: ").append(daemon ? "present" : "MISSING").append('\n');
                if (!daemon) { out.append("Action: install a manager APK containing libksud.so.\n"); missing++; }
            } catch (PackageManager.NameNotFoundException error) {
                out.append("Action: selected manager is no longer installed; select another.\n");
                missing++;
            }
        }
        out.append("Prerequisites: ").append(missing == 0 ? "files found" : "attention needed").append('\n');
        out.append("File checks do not verify driver signatures, vendor compatibility, or exploit success.\n");
        return out.toString();
    }

    static String kmiHint(String kernel) {
        Matcher match = Pattern.compile("^(\\d+)\\.(\\d+)\\..*?-(android\\d+)(?:-|$)").matcher(kernel);
        return match.find() ? match.group(3) + "-" + match.group(1) + "." + match.group(2)
                : "unavailable in kernel release; native run log determines selection";
    }

    static String recentOutput(String log) {
        if (log == null || log.trim().isEmpty()) return "No run log recorded yet.\n";
        String[] lines = log.trim().split("\\r?\\n");
        StringBuilder out = new StringBuilder("LAST OBSERVED RUN OUTPUT\n");
        for (int i = Math.max(0, lines.length - 12); i < lines.length; i++)
            out.append(lines[i]).append('\n');
        out.append("See the complete log below for the failing stage and context.\n");
        return out.toString();
    }
}
