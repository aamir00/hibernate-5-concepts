package h5.concepts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;

import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.export.common.AbstractExporter;
import org.hibernate.tool.internal.export.common.GenericExporter;
import org.hibernate.tool.internal.export.java.POJOClass;

import h5.concepts.support.DemoMetaDataDialect;
import h5.concepts.support.Out;
import h5.concepts.support.RevengSupport;
import h5.concepts.support.DemoDatabase;

/**
 * Rows 55–58: Hibernate Tools hbm2x template exporters. A {@link GenericExporter} runs a FreeMarker template
 * once per reverse-engineered entity ({@link POJOClass}) and writes one file each.
 * <p>
 * In wmstdappdbimpl only {@code WMGenericExporter} (and one other class) use this, and nothing calls them
 * (dead code). Its {@code resolveFilename(String)} does not override {@code GenericExporter.resolveFilename(POJOClass)};
 * {@link SummaryExporter} below shows both methods, and which one Tools actually calls.
 * <p>
 * Tools 6 moved the exporters from {@code org.hibernate.tool.hbm2x} to {@code org.hibernate.tool.internal.export.*}
 * and replaced their setters with properties keyed by {@link ExporterConstants}.
 */
public class ExporterMain {

    static final Path OUTPUT_DIR = Path.of("build", "export-out");

    public static void main(String[] args) throws IOException {
        Out.banner("ExporterMain — AbstractExporter, GenericExporter.resolveFilename(POJOClass), POJOClass");
        DemoDatabase.ensureCreated();

        Out.row("55", "AbstractExporter — output directory, metadata descriptor, template path, properties");
        MetadataDescriptor descriptor = RevengSupport.descriptor(
            RevengSupport.strategy(), RevengSupport.properties(DemoMetaDataDialect.class));
        SummaryExporter exporter = new SummaryExporter();
        Properties properties = exporter.getProperties();
        properties.put(ExporterConstants.DESTINATION_FOLDER, OUTPUT_DIR.toFile());
        properties.put(ExporterConstants.METADATA_DESCRIPTOR, descriptor);
        properties.put(ExporterConstants.TEMPLATE_PATH, new String[0]); // templates are found on the classpath
        properties.setProperty("jdk5", "true");
        Out.kv("exporter is an AbstractExporter", exporter instanceof AbstractExporter);
        Out.kv("DESTINATION_FOLDER", ((File) properties.get(ExporterConstants.DESTINATION_FOLDER)).getAbsolutePath());
        Out.note("CHANGED in 6: the setters (setOutputDirectory, setMetadataDescriptor, setTemplatePath, setTemplateName, "
            + "setFilePattern, setForEach) were removed -> exporter.getProperties().put(ExporterConstants.X, value); "
            + "getOutputDirectory()/getTemplateName() are now protected and getFilePattern() private.");

        Out.row("56", "GenericExporter — template + file pattern, for-each entity");
        properties.put(ExporterConstants.TEMPLATE_NAME, "templates/entity-summary.ftl");
        properties.put(ExporterConstants.FILE_PATTERN, "{package-name}/{class-name}.summary.txt");
        properties.put(ExporterConstants.FOR_EACH, "entity");
        Out.kv("TEMPLATE_NAME", properties.get(ExporterConstants.TEMPLATE_NAME));
        Out.kv("FILE_PATTERN", properties.get(ExporterConstants.FILE_PATTERN));
        Out.line("exporter.start() ...");
        try {
            exporter.start();
        } finally {
            ((StandardServiceRegistryImpl) ((MetadataImpl) exporter.getMetadata()).getBootstrapContext().getServiceRegistry()).destroy();
        }
        Out.kv("resolveFilename(POJOClass) calls", exporter.pojoOverrideCalls);
        Out.kv("resolveFilename(String) calls", exporter.stringOverloadCalls
            + "   (an overload, not an override: Tools never calls it — same as WMGenericExporter)");

        Out.row("56", "Files written");
        try (Stream<Path> files = Files.walk(OUTPUT_DIR)) {
            files.filter(Files::isRegularFile).sorted().forEach(f -> Out.line("  " + f));
        }
        Path sample = OUTPUT_DIR.resolve("com/demo/hr/EmployeeEntity.summary.txt");
        Out.line("Content of " + sample + ":");
        Files.readAllLines(sample).forEach(l -> Out.line("  | " + l));
        Out.note("CHANGED in 6: POJOClass.getQualifiedDeclarationName() repeats the package (com.demo.hr.com.demo.hr.X). "
            + "Tools 5.6 had an inverted check that returned the already-qualified class name; Tools 6 fixed the check and "
            + "now prefixes the package to that qualified name. getPackageName() + \".\" + getShortName() is the safe form. "
            + "A DATE column maps to java.util.Date in generated code, as in 5.x (Tools 6.6 generated java.sql.Date).");

        Out.row("58", "org.hibernate.tool.hbm2x.ant.ConfigurationTask");
        Out.line("Only a stale Javadoc @see in wmstdappdbimpl's ConfigurationBuilder; the class is not in Tools 5.6.15 (nor 6.6). Nothing to run.");
    }

    /** [Rows 56, 57] GenericExporter subclass that controls the output path per entity. */
    static class SummaryExporter extends GenericExporter {

        int pojoOverrideCalls;
        int stringOverloadCalls;

        /** The real extension point: called once per entity with its POJOClass. */
        @Override
        protected String resolveFilename(POJOClass element) {
            pojoOverrideCalls++;
            String filename = super.resolveFilename(element);
            Out.row("57", "POJOClass  " + element.getDeclarationName());
            Out.kv("getQualifiedDeclarationName()", element.getQualifiedDeclarationName());
            Out.kv("getPackageName() / getShortName()", element.getPackageName() + " / " + element.getShortName());
            Out.kv("isComponent() / hasIdentifierProperty()", element.isComponent() + " / " + element.hasIdentifierProperty());
            Out.kv("getDecoratedObject()", Out.simpleName(element.getDecoratedObject()));
            Out.kv("resolveFilename -> ", filename);
            return filename;
        }

        /** Same signature as WMGenericExporter's method: compiles, but GenericExporter never calls it. */
        protected String resolveFilename(String pattern) {
            stringOverloadCalls++;
            return new File(pattern).getName();
        }
    }
}
