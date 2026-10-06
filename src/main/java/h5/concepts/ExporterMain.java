package h5.concepts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.hbm2x.AbstractExporter;
import org.hibernate.tool.hbm2x.GenericExporter;
import org.hibernate.tool.hbm2x.pojo.POJOClass;

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
        exporter.setOutputDirectory(OUTPUT_DIR.toFile());
        exporter.setMetadataDescriptor(descriptor);
        exporter.setTemplatePath(new String[0]); // templates are found on the classpath
        exporter.getProperties().setProperty("jdk5", "true");
        Out.kv("exporter is an AbstractExporter", exporter instanceof AbstractExporter);
        Out.kv("getOutputDirectory()", exporter.getOutputDirectory().getAbsolutePath());

        Out.row("56", "GenericExporter — template + file pattern, for-each entity");
        exporter.setTemplateName("templates/entity-summary.ftl");
        exporter.setFilePattern("{package-name}/{class-name}.summary.txt");
        exporter.setForEach("entity");
        Out.kv("getTemplateName()", exporter.getTemplateName());
        Out.kv("getFilePattern()", exporter.getFilePattern());
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

        Out.row("58", "org.hibernate.tool.hbm2x.ant.ConfigurationTask");
        Out.line("Only a stale Javadoc @see in wmstdappdbimpl's ConfigurationBuilder; the class is not in Tools 5.6.15. Nothing to run.");
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
