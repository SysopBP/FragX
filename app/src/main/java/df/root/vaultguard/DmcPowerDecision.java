package df.root.vaultguard;

import java.util.concurrent.atomic.AtomicBoolean;

/** One power request can complete once, and never after its monotonic deadline. */
public final class DmcPowerDecision {
    private final long deadline;
    private final AtomicBoolean settled = new AtomicBoolean(false);
    public DmcPowerDecision(long deadline) { this.deadline = deadline; }
    public boolean cancel() { return settled.compareAndSet(false, true); }
    public boolean complete(long now) {
        if (now >= deadline) { cancel(); return false; }
        return settled.compareAndSet(false, true);
    }
}
