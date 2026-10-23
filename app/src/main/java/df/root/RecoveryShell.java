package df.root;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.FutureTask;

/** Only fixed read-only status and explicit reboot commands; never used to obtain root. */
final class RecoveryShell {
    static String run(String command) {
        Process process = null;
        try {
            process = startSu(command);
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
    private static Process startSu(String command) throws java.io.IOException {
        // Java may reject a missing executable before KernelSU's exec hook
        // sees it. Let Android's shell issue exec instead. Retry only missing
        // or non-executable entry points, never permission denial or timeout.
        // Try exec directly first: some KernelSU configurations expose su
        // through the exec hook without placing it on disk. Keep the shell
        // fallback for runtimes which reject missing executables in Java.
        for (String su : new String[]{"/system/bin/su", "/system/xbin/su",
                "/sbin/su", "/debug_ramdisk/su", "su"}) {
            try {
                return new ProcessBuilder(su, "-c", command).redirectErrorStream(true).start();
            } catch (java.io.IOException missingExecutable) {
                // No child started; trying another entry point cannot repeat the command.
            }
        }
        String script = "";
        for (String su : new String[]{"/system/bin/su", "/system/xbin/su",
                "/sbin/su", "/debug_ramdisk/su", "su"}) {
            script += "out=$(" + su + " -c \"$1\" 2>&1); rc=$?; "
                    + "if [ \"$rc\" -ne 126 ] && [ \"$rc\" -ne 127 ]; then "
                    + "printf '%s\\n' \"$out\"; exit \"$rc\"; fi; ";
        }
        script += "printf '%s\\n' \"$out\"; exit \"$rc\"";
        return new ProcessBuilder("/system/bin/sh", "-c", script, "fragx-su", command)
                .redirectErrorStream(true).start();
    }
    static boolean uidZero(String result) { return "0".equals(result.trim()); }
    private RecoveryShell() {}
}
