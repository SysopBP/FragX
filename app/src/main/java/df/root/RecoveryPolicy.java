package df.root;
/** Fail closed after an interrupted attempt; duplicate boot broadcasts never start another. */
final class RecoveryPolicy {
    static boolean mayAutorun(boolean pending, boolean failed, int boot, int attemptedBoot) {
        return !pending && !failed && !(boot >= 0 && boot == attemptedBoot);
    }
    private RecoveryPolicy() {}
}
