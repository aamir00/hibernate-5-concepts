package h5.concepts.support;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Console narration helpers. Every concept is printed as
 * <pre>
 * === [Row 11] Table.getColumnIterator() ===
 *   key : value
 * </pre>
 * where "Row N" is the row number in {@code ../hibernate-classes-and-concepts.md}.
 */
public final class Out {

    /**
     * Hibernate Tools 7 logs through java.util.logging (Tools 5.6 used jboss-logging), at INFO for every table it
     * binds. Kept as a field so the JUL logger (and its settings) is not garbage collected.
     */
    private static final Logger TOOLS_JUL_LOGGER = Logger.getLogger("org.hibernate.tool");

    static {
        // Hibernate logs through jboss-logging; route it to slf4j-simple (configured at WARN in
        // simplelogger.properties). This runs before any Hibernate logger is created because every
        // Main calls Out.banner(..) first.
        System.setProperty("org.jboss.logging.provider", "slf4j");
        // Tools' JUL output: WARNING and above only, printed to stdout in the same "LEVEL logger - message" shape as
        // slf4j-simple, so it stays in order with the narration and has no timestamps.
        TOOLS_JUL_LOGGER.setLevel(Level.WARNING);
        TOOLS_JUL_LOGGER.setUseParentHandlers(false);
        TOOLS_JUL_LOGGER.addHandler(new Handler() {
            private final SimpleFormatter formatter = new SimpleFormatter();

            @Override
            public void publish(LogRecord record) {
                if (isLoggable(record)) {
                    String logger = record.getLoggerName();
                    System.out.println(record.getLevel() + " " + logger.substring(logger.lastIndexOf('.') + 1)
                        + " - " + formatter.formatMessage(record));
                }
            }

            @Override
            public void flush() {
                System.out.flush();
            }

            @Override
            public void close() {
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
