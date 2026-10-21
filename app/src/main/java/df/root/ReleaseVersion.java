package df.root;

/** Version ordering for published stable and preview releases. */
final class ReleaseVersion {
    private ReleaseVersion() {}

    static boolean valid(String value) {
        return value != null && value.length() <= 100
                && value.matches("[vV]?[0-9]+(?:\\.[0-9]+){1,3}(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?");
    }

    static int compare(String left, String right) {
        String a = left.replaceFirst("^[vV]", "").split("\\+", 2)[0];
        String b = right.replaceFirst("^[vV]", "").split("\\+", 2)[0];
        String[] ap = a.split("-", 2), bp = b.split("-", 2);
        String[] ac = ap[0].split("\\."), bc = bp[0].split("\\.");
        for (int i = 0; i < Math.max(ac.length, bc.length); i++) {
            int order = number(i < ac.length ? ac[i] : "0", i < bc.length ? bc[i] : "0");
            if (order != 0) return order;
        }
        if (ap.length != bp.length) return ap.length == 1 ? 1 : -1;
        if (ap.length == 1) return 0;
        String[] ax = ap[1].split("\\."), bx = bp[1].split("\\.");
        for (int i = 0; i < Math.min(ax.length, bx.length); i++) {
            boolean an = ax[i].matches("[0-9]+"), bn = bx[i].matches("[0-9]+");
            int order = an && bn ? number(ax[i], bx[i])
                    : an != bn ? (an ? -1 : 1) : ax[i].compareTo(bx[i]);
            if (order != 0) return order;
        }
        return Integer.compare(ax.length, bx.length);
    }

    private static int number(String a, String b) {
        return new java.math.BigInteger(a).compareTo(new java.math.BigInteger(b));
    }
}
