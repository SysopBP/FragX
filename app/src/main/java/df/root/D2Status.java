package df.root;

import android.content.Context;
import df.root.vaultguard.DmcRecovery;

/** Actual VaultGuard status. Never infer Samsung vault contents from Android credentials. */
final class D2Status {
    enum Dot { GREY, GREEN, RED }
    static final class Result {
        final Dot dot;
        final String detail;
        Result(Dot dot, String detail) { this.dot = dot; this.detail = detail; }
    }
    static Result read(Context context) {
        DmcRecovery.Status s = DmcRecovery.INSTANCE.status(context);
        // Failure/timeout is unknown, never evidence that Download Mode is blocked.
        Dot dot = s.getTargetsMet() ? Dot.GREEN
                : Boolean.FALSE.equals(s.getDownloadAllowed()) ? Dot.RED : Dot.GREY;
        return new Result(dot, s.report());
    }
    private D2Status() {}
}
