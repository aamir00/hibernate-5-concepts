package h5.concepts;

import java.util.LinkedHashMap;
import java.util.Map;

import h5.concepts.support.Out;

/**
 * Runs every Main in inventory order and prints a summary. Default main class of {@code ./gradlew run}.
 * Each Main can also be run on its own.
 */
public class RunAllMain {

    @FunctionalInterface
    interface Demo {
        void run(String[] args) throws Exception;
    }

    public static void main(String[] args) {
        Out.banner("RunAllMain — every Hibernate 5.6.15 concept used by wmstdappdbimpl");
        Map<String, Demo> demos = new LinkedHashMap<>();
        demos.put("BootstrapMain        rows 1-7", BootstrapMain::main);
        demos.put("MappingModelMain     rows 8-18", MappingModelMain::main);
        demos.put("EntityHqlMain        rows 19-20 (+29-34)", EntityHqlMain::main);
        demos.put("DialectsMain         rows 21-28", DialectsMain::main);
        demos.put("NativeSqlSessionMain rows 29-37", NativeSqlSessionMain::main);
        demos.put("RevengStrategyMain   rows 39-47, 53-54", RevengStrategyMain::main);
        demos.put("MetaDataDialectMain  rows 38, 48-52", MetaDataDialectMain::main);
        demos.put("ExporterMain         rows 55-58", ExporterMain::main);

        Map<String, String> results = new LinkedHashMap<>();
        for (Map.Entry<String, Demo> demo : demos.entrySet()) {
            long start = System.nanoTime();
            try {
                demo.getValue().run(args);
                results.put(demo.getKey(), "OK     " + (System.nanoTime() - start) / 1_000_000 + " ms");
            } catch (Throwable t) {
                results.put(demo.getKey(), "FAILED " + t);
                t.printStackTrace();
            }
        }

        Out.banner("Summary");
        results.forEach((name, result) -> System.out.printf("  %-42s %s%n", name, result));
        if (results.values().stream().anyMatch(r -> r.startsWith("FAILED"))) {
            System.exit(1);
        }
    }
}
