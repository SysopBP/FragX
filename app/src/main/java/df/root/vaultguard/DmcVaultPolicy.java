package df.root.vaultguard;

import java.util.Arrays;

/** DMC slot 1 layout observed in the supplied JBCompanion APK. */
public final class DmcVaultPolicy {
    public interface Vault {
        byte[] read() throws Exception;
        int write(byte[] data) throws Exception;
    }

    public static void validate(byte[] data) {
        if (data == null)
            throw new IllegalStateException("DMC read returned no record; vault access or initialization must be checked");
        if (data.length != 32)
            throw new IllegalStateException("Unsupported DMC record length: " + data.length + " bytes (expected 32)");
        for (int i = 0; i < 3; i++) {
            if (data[i] != 0 && data[i] != 1)
                throw new IllegalStateException("Unsupported DMC flag value");
        }
    }

    public static boolean matchesTarget(byte[] data) {
        validate(data);
        return data[0] == 0 && data[1] == 0 && data[2] == 1;
    }

    public static boolean downloadAllowed(byte[] data) {
        validate(data);
        return data[0] == 0 || data[1] == 1 || data[2] == 1;
    }

    public static byte[] enforce(Vault vault) throws Exception {
        byte[] before = vault.read();
        validate(before);
        if (matchesTarget(before)) return before;
        byte[] target = before.clone();
        target[0] = 0;
        target[1] = 0;
        target[2] = 1;
        int rc = vault.write(target);
        if (rc != 0) throw new IllegalStateException("DMC write rejected (" + rc + ")");
        byte[] after = vault.read();
        validate(after);
        if (!matchesTarget(after) || !Arrays.equals(target, after))
            throw new IllegalStateException("DMC read-back verification failed");
        return after;
    }

    private DmcVaultPolicy() { }
}
