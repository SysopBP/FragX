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
            return "Unavailable: " + e.getClass().getSimpleName();
        } finally {
            if (process != null) {
                process.destroy();
                try { process.getInputStream().close(); } catch (Exception ignored) { }
            }
        }
    }
    private static Process startSu(String command) throws java.io.IOException {
        // Use an absolute path first: KernelSU can handle su at exec time even
        // when Java's PATH search cannot find an on-disk su executable.
        java.io.IOException last = null;
        for (String su : new String[]{"/system/bin/su", "/system/xbin/su",
                "/sbin/su", "/debug_ramdisk/su", "su"}) {
            try {
                return new ProcessBuilder(su, "-c", command).redirectErrorStream(true).start();
            } catch (java.io.IOException e) {
                last = e;
            }
        }
        throw new java.io.IOException("No root shell could be started", last);
    }
    static boolean uidZero(String result) { return "0".equals(result.trim()); }
    private RecoveryShell() {}
}
