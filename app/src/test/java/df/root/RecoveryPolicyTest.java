package df.root;
import org.junit.Test;
import static org.junit.Assert.*;
public class RecoveryPolicyTest {
    @Test public void interruptedAttemptBlocksEvenOnNextBoot() {
        assertFalse(RecoveryPolicy.mayAutorun(true, false, 101, 100));
    }
    @Test public void failedAttemptBlocksAndExplicitRearmAllows() {
        assertFalse(RecoveryPolicy.mayAutorun(false, true, 101, 100));
        assertTrue(RecoveryPolicy.mayAutorun(false, false, 101, -2));
    }
    @Test public void duplicateBootDeliveryBlockedAndNewBootAllowed() {
        assertFalse(RecoveryPolicy.mayAutorun(false, false, 100, 100));
        assertTrue(RecoveryPolicy.mayAutorun(false, false, 101, 100));
    }
    @Test public void unavailableBootCounterStillHonorsPersistentInterlock() {
        assertFalse(RecoveryPolicy.mayAutorun(true, false, -1, -1));
        assertTrue(RecoveryPolicy.mayAutorun(false, false, -1, -2));
    }
}
