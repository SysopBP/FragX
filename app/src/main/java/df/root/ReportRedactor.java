package df.root;

/** Best-effort removal of common identifiers and credentials before export. */
final class ReportRedactor {
    static String redact(String input) {
        return input
                .replaceAll("(?im)^([^\\r\\n]*(?:imei|imsi|serial(?:no|number)?|android[_ ]?id|password|token|secret|aes.key)[^:=\\r\\n]*[:=])[^\\r\\n]*", "$1 [redacted]")
                .replaceAll("(?i)(?:[0-9a-f]{2}:){5}[0-9a-f]{2}", "[MAC redacted]")
                .replaceAll("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b", "[email redacted]")
                .replaceAll("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b", "[IP redacted]");
    }
    private ReportRedactor() {}
}
