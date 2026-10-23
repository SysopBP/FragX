package df.root;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.FutureTask;

/** Fixed status and explicit reboot commands through the selected manager's authorized shell. */
final class RecoveryShell {
    static String run(android.content.Context context, String command) {
        return run(context, command, false);
    }

    private static String run(android.content.Context context, String command, boolean globalMnt) {
        Process process = null;
        try {
            process = startSu(context, command, globalMnt);
            final Process child = process;
            FutureTask<String> read = new FutureTask<>(() -> {
                // Bound captured output while continuing to drain the pipe.
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                for (int n; (n = child.getInputStream().read(buffer)) != -1; )
                    if (out.size() < 16384) out.write(buffer, 0, Math.min(n, 16384 - out.size()));
                return out.toString(StandardCharsets.UTF_8.name());
            });
            Thread reader = new Thread(read, "fragx-status-reader");
            reader.setDaemon(true);
            reader.start();
            if (!process.waitFor(8, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "Unavailable: root shell timed out or permission request pending";
            }
            String output = read.get(1, TimeUnit.SECONDS).trim();
            return process.exitValue() == 0 ? output : "Unavailable (exit " + process.exitValue() + "): " + output;
        } catch (Exception e) {
            return "Unavailable: " + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : ": " + e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
                try { process.getInputStream().close(); } catch (Exception ignored) { }
            }
        }
    }
    private static String daemonPath(android.content.Context context) throws Exception {
        String manager = context.createDeviceProtectedStorageContext()
                .getSharedPreferences(ExploitRunner.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(ExploitRunner.PREF_SU_MANAGER, null);
        if (manager == null) throw new java.io.IOException("No SU manager selected");
        android.content.pm.ApplicationInfo app = context.getPackageManager()
                .getApplicationInfo(manager, 0);
        if (!ExploitRunner.hasKsud(app))
            throw new java.io.IOException("Selected manager has no bundled libksud.so");
        return new java.io.File(app.nativeLibraryDir, "libksud.so").getAbsolutePath();
    }

    private static Process startSu(android.content.Context context, String command,
                                   boolean globalMnt) throws Exception {
        // Match KitsuSU's CLI shell transport. debug su takes no -c argument:
        // it grants the caller's existing KernelSU authorization and execs sh.
        // Keep denial/timeouts visible; do not retry a reboot through another shell.
        String daemon = daemonPath(context);
        ProcessBuilder builder = globalMnt
                ? new ProcessBuilder(daemon, "debug", "su", "-g")
                : new ProcessBuilder(daemon, "debug", "su");
        Process process = builder.redirectErrorStream(true).start();
        try {
            try (java.io.OutputStream input = process.getOutputStream()) {
                input.write(("if [ \"$(id -u)\" != 0 ]; then "
                        + "echo 'Root authorization unavailable'; exit 1; fi\n"
                        + command + "\nexit $?\n").getBytes(StandardCharsets.UTF_8));
            }
            return process;
        } catch (Exception error) {
            process.destroyForcibly();
            throw error;
        }
    }

    static String reboot(android.content.Context context, boolean soft) {
        try {
            String command = soft
                    ? quote(daemonPath(context)) + " soft-reboot"
                    : "/system/bin/svc power reboot || /system/bin/reboot";
            return run(context, command, soft);
        } catch (Exception error) {
            return "Unavailable: " + error.getMessage();
        }
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
    static boolean uidZero(String result) { return "0".equals(result.trim()); }
    private RecoveryShell() {}
}
