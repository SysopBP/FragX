package df.root.vaultguard;

import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class DmcPowerDecisionTest {
    @Test public void approvalIsSingleUse() {
        DmcPowerDecision gate = new DmcPowerDecision(5000);
        assertTrue(gate.complete(4999));
        assertFalse(gate.complete(4999));
    }
    @Test public void deadlineAndLateReadCannotResume() {
        assertFalse(new DmcPowerDecision(5000).complete(5000));
        assertFalse(new DmcPowerDecision(5000).complete(6000));
    }
    @Test public void cancellationPreventsDelayedRestart() {
        DmcPowerDecision gate = new DmcPowerDecision(5000);
        assertTrue(gate.cancel());
        assertFalse(gate.complete(1000));
        assertFalse(gate.cancel());
    }
    @Test public void concurrentCompletionsCannotRestartTwice() throws Exception {
        DmcPowerDecision gate = new DmcPowerDecision(5000);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger approved = new AtomicInteger();
        Thread[] workers = new Thread[16];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Thread(() -> {
                try { start.await(); } catch (InterruptedException e) { throw new AssertionError(e); }
                if (gate.complete(1000)) approved.incrementAndGet();
            });
            workers[i].start();
        }
        start.countDown();
        for (Thread worker : workers) worker.join();
        assertEquals(1, approved.get());
    }
}
