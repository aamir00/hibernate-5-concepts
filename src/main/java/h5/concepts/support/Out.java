package h5.concepts.support;

/**
 * Console narration helpers. Every concept is printed as
 * <pre>
 * === [Row 11] Table.getColumnIterator() ===
 *   key : value
 * </pre>
 * where "Row N" is the row number in {@code ../hibernate-classes-and-concepts.md}.
 */
public final class Out {

    static {
        // Hibernate logs through jboss-logging; route it to slf4j-simple (configured at WARN in
        // simplelogger.properties). This runs before any Hibernate logger is created because every
        // Main calls Out.banner(..) first.
        System.setProperty("org.jboss.logging.provider", "slf4j");
    }

    private Out() {
    }

    public static void banner(String title) {
        String bar = "#".repeat(100);
        System.out.println();
        System.out.println(bar);
        System.out.println("#  " + title);
        System.out.println(bar);
    }

    public static void row(String rows, String title) {
        System.out.println();
        System.out.println("=== [Row " + rows + "] " + title + " ===");
    }

    public static void kv(String key, Object value) {
        System.out.printf("  %-46s : %s%n", key, value);
    }

    public static void line(String text) {
        System.out.println("  " + text);
    }

    public static void note(String text) {
        System.out.println("  NOTE: " + text);
    }

    public static String simpleName(Object o) {
        return o == null ? "null" : o.getClass().getName();
    }
}
