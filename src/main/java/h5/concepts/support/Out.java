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

    /** Hibernate Tools 6+ logs through java.util.logging at INFO; held here so the level set below sticks. */
    private static final java.util.logging.Logger TOOLS_LOGGER = java.util.logging.Logger.getLogger("org.hibernate.tool");

    static {
        // Hibernate logs through jboss-logging; route it to slf4j-simple (configured at WARN in
        // simplelogger.properties). This runs before any Hibernate logger is created because every
        // Main calls Out.banner(..) first.
        System.setProperty("org.jboss.logging.provider", "slf4j");
        // Keep only warnings from Tools' reverse-engineering binders (they log every table at INFO), and print them on
        // stdout (java.util.logging's console handler uses stderr) so they stay in order with the narration.
        TOOLS_LOGGER.setLevel(java.util.logging.Level.WARNING);
        TOOLS_LOGGER.setUseParentHandlers(false);
        TOOLS_LOGGER.addHandler(new java.util.logging.StreamHandler(System.out, new java.util.logging.SimpleFormatter()) {
            @Override
            public synchronized void publish(java.util.logging.LogRecord record) {
                super.publish(record);
                flush();
            }
        });
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
