package df.root.vaultguard;

import org.junit.Test;
import static org.junit.Assert.*;

public class DmcVaultPolicyTest {
    static final class FakeVault implements DmcVaultPolicy.Vault {
        byte[] data = new byte[32];
        int writes;
        int rc;
        boolean ignoreWrite;
        boolean alterOtherByte;
        public byte[] read() { return data == null ? null : data.clone(); }
        public int write(byte[] target) {
            writes++;
            if (rc == 0 && !ignoreWrite) {
                data = target.clone();
                if (alterOtherByte) data[20]++;
            }
            return rc;
        }
    }

    @Test public void enforcesTargetsWithoutTouchingRemainingRecord() throws Exception {
        FakeVault vault = new FakeVault();
        vault.data[0] = 1;
        vault.data[1] = 1;
        for (int i = 3; i < 32; i++) vault.data[i] = (byte)(i * 7);
        byte[] original = vault.data.clone();
        byte[] result = DmcVaultPolicy.enforce(vault);
        assertTrue(DmcVaultPolicy.matchesTarget(result));
        assertTrue(DmcVaultPolicy.downloadAllowed(result));
        for (int i = 3; i < 32; i++) assertEquals(original[i], result[i]);
        assertEquals(1, vault.writes);
        DmcVaultPolicy.enforce(vault);
        assertEquals("Already-correct records must not be rewritten", 1, vault.writes);
    }

    @Test public void rejectsUnrecognizedRecordsWithoutWriting() {
        FakeVault vault = new FakeVault();
        vault.data = new byte[31];
        assertThrows(IllegalStateException.class, () -> DmcVaultPolicy.enforce(vault));
        assertEquals(0, vault.writes);
        vault.data = new byte[32];
        vault.data[0] = 2;
        assertThrows(IllegalStateException.class, () -> DmcVaultPolicy.enforce(vault));
        assertEquals(0, vault.writes);
        vault.data = null;
        assertThrows(IllegalStateException.class, () -> DmcVaultPolicy.enforce(vault));
        assertEquals(0, vault.writes);
    }

    @Test public void distinguishesMissingRecordsFromUnsupportedLengths() {
        IllegalStateException missing = assertThrows(IllegalStateException.class,
            () -> DmcVaultPolicy.validate(null));
        assertTrue(missing.getMessage().contains("no record"));
        for (int length : new int[] {0, 3, 31, 33, 64}) {
            FakeVault vault = new FakeVault();
            vault.data = new byte[length];
            IllegalStateException unsupported = assertThrows(IllegalStateException.class,
                () -> DmcVaultPolicy.enforce(vault));
            assertTrue(unsupported.getMessage().contains(length + " bytes"));
            assertEquals(0, vault.writes);
        }
    }

    @Test public void neverReportsRejectedOrUnverifiedWritesAsSuccess() {
        FakeVault vault = new FakeVault();
        vault.rc = -1;
        assertThrows(IllegalStateException.class, () -> DmcVaultPolicy.enforce(vault));
        vault.rc = 0;
        vault.ignoreWrite = true;
        assertThrows(IllegalStateException.class, () -> DmcVaultPolicy.enforce(vault));
        vault.ignoreWrite = false;
        vault.alterOtherByte = true;
        assertThrows(IllegalStateException.class, () -> DmcVaultPolicy.enforce(vault));
    }

    @Test public void derivesDownloadStatusFromAllFlagCombinations() {
        for (int lock = 0; lock <= 1; lock++)
            for (int maint = 0; maint <= 1; maint++)
                for (int at = 0; at <= 1; at++) {
                    byte[] data = new byte[32];
                    data[0] = (byte)lock; data[1] = (byte)maint; data[2] = (byte)at;
                    assertEquals(!(lock == 1 && maint == 0 && at == 0), DmcVaultPolicy.downloadAllowed(data));
                    assertEquals(lock == 0 && maint == 0 && at == 1, DmcVaultPolicy.matchesTarget(data));
                }
    }
}
